/**
 * Cloudflare Worker Push Relay for BaagbaanBoi Chat
 *
 * Ports the exact push notification behavior of `functions/index.js` (`onMessageCreated`)
 * to run free on Cloudflare Workers without Firebase Cloud Functions:
 *  1. Authenticates the sender via their Firebase Auth ID token (`Authorization: Bearer <idToken>`).
 *  2. Reads `chats/{chatId}/messages/{messageId}` (with `updateTime`) from Firestore REST API.
 *  3. Verifies caller UID === `message.senderId`, skips if `messageType === "call"`,
 *     and verifies the message timestamp is within the last 180 seconds (`{skipped:"stale"}`).
 *  4. Reads `chats/{chatId}`, verifies caller UID is in `chat.participantIds` (else 403),
 *     skips if `blockedBy` is non-empty, and finds recipients (`participantIds` minus `senderId`).
 *  5. Claims `pushedAt` on the message document BEFORE sending FCM using the
 *     `currentDocument.updateTime` precondition; a failed claim returns 200 `{skipped:"duplicate"}`.
 *  6. Computes `senderName` (`displayName` -> `@username` -> `"New message"`) and
 *     message `body` (text truncated to 100 chars + `"..."`, or attachment label
 *     `"Photo"`, `"Video"`, `"Voice message"`, `"Document"`, `"Location"`, `"Contact"`).
 *  7. Sends high-priority FCM HTTP v1 notifications (`channel_id: "chat_messages"`, `tag: chatId`)
 *     to each recipient's `users/{uid}.fcmTokens` and removes unregistered/invalid tokens.
 *
 * Required Worker Environment Variables / Secrets:
 *  - FIREBASE_PROJECT_ID: "baagbaan-boi-20" (default if omitted)
 *  - FIREBASE_SERVICE_ACCOUNT_JSON: Full JSON string of a Firebase service account key
 */

const DEFAULT_PROJECT_ID = "baagbaan-boi-20";
const MAX_MESSAGE_AGE_SECONDS = 180;
const GOOGLE_SECURETOKEN_JWK_URL =
  "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";
const GOOGLE_OAUTH_TOKEN_URL = "https://oauth2.googleapis.com/token";
const OAUTH_SCOPES = [
  "https://www.googleapis.com/auth/datastore",
  "https://www.googleapis.com/auth/firebase.messaging",
].join(" ");

// In-memory caches across warm Worker invocations
let cachedGoogleJwks = { keys: [], expiresAtMs: 0 };
let cachedAccessToken = { token: "", expiresAtMs: 0, cacheKey: "" };

export function __resetWorkerCachesForTests() {
  cachedGoogleJwks = { keys: [], expiresAtMs: 0 };
  cachedAccessToken = { token: "", expiresAtMs: 0, cacheKey: "" };
}

function jsonResponse(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type, Authorization",
    },
  });
}

function base64UrlToUint8Array(base64Url) {
  const padding = "=".repeat((4 - (base64Url.length % 4)) % 4);
  const base64 = (base64Url + padding).replace(/-/g, "+").replace(/_/g, "/");
  const raw = atob(base64);
  const out = new Uint8Array(raw.length);
  for (let i = 0; i < raw.length; i++) {
    out[i] = raw.charCodeAt(i);
  }
  return out;
}

function uint8ArrayToBase64Url(bytes) {
  let binary = "";
  for (let i = 0; i < bytes.length; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

function stringToBase64Url(str) {
  return uint8ArrayToBase64Url(new TextEncoder().encode(str));
}

function decodeJwtPart(part) {
  const bytes = base64UrlToUint8Array(part);
  return JSON.parse(new TextDecoder().decode(bytes));
}

function pemToArrayBuffer(pem) {
  const normalized = pem
    .replace(/\\n/g, "\n")
    .replace(/-----BEGIN [A-Z0-9 ]+-----/g, "")
    .replace(/-----END [A-Z0-9 ]+-----/g, "")
    .replace(/\s+/g, "");
  const binary = atob(normalized);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes.buffer;
}

async function getGooglePublicJwks(forceRefresh = false) {
  const now = Date.now();
  if (!forceRefresh && cachedGoogleJwks.keys.length > 0 && now < cachedGoogleJwks.expiresAtMs) {
    return cachedGoogleJwks.keys;
  }

  const res = await fetch(GOOGLE_SECURETOKEN_JWK_URL);
  if (!res.ok) {
    throw new Error(`Failed to fetch Google JWK set: HTTP ${res.status}`);
  }

  let maxAgeSec = 3600;
  const cacheControl = res.headers.get("cache-control") || "";
  const match = cacheControl.match(/max-age=(\d+)/i);
  if (match) {
    maxAgeSec = Math.max(60, parseInt(match[1], 10));
  }

  const data = await res.json();
  const keys = Array.isArray(data?.keys) ? data.keys : [];
  cachedGoogleJwks = {
    keys,
    expiresAtMs: now + maxAgeSec * 1000,
  };
  return keys;
}

/**
 * Verifies a Firebase Auth ID token (RS256 JWT) and returns the authenticated `uid`.
 */
export async function verifyFirebaseIdToken(idToken, projectId) {
  if (typeof idToken !== "string" || idToken.trim().length === 0) {
    throw new Error("Empty ID token");
  }

  const parts = idToken.trim().split(".");
  if (parts.length !== 3) {
    throw new Error("Malformed JWT");
  }

  const [encodedHeader, encodedPayload, encodedSignature] = parts;
  const header = decodeJwtPart(encodedHeader);
  const payload = decodeJwtPart(encodedPayload);

  if (header.alg !== "RS256" || typeof header.kid !== "string" || !header.kid) {
    throw new Error("Unsupported JWT algorithm or missing kid");
  }

  const nowSec = Math.floor(Date.now() / 1000);
  const expectedIssuer = `https://securetoken.google.com/${projectId}`;

  if (payload.aud !== projectId) {
    throw new Error("Invalid audience claim");
  }
  if (payload.iss !== expectedIssuer) {
    throw new Error("Invalid issuer claim");
  }
  if (typeof payload.sub !== "string" || payload.sub.trim().length === 0) {
    throw new Error("Invalid subject (sub) claim");
  }
  if (typeof payload.exp !== "number" || payload.exp <= nowSec) {
    throw new Error("ID token has expired");
  }
  if (typeof payload.iat !== "number" || payload.iat > nowSec + 60) {
    throw new Error("ID token issued in the future");
  }

  let keys = await getGooglePublicJwks(false);
  let jwk = keys.find((k) => k.kid === header.kid);
  if (!jwk) {
    keys = await getGooglePublicJwks(true);
    jwk = keys.find((k) => k.kid === header.kid);
  }
  if (!jwk) {
    throw new Error("No matching Google JWK found for kid");
  }

  const cryptoKey = await crypto.subtle.importKey(
    "jwk",
    jwk,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["verify"]
  );

  const signedContent = new TextEncoder().encode(`${encodedHeader}.${encodedPayload}`);
  const signatureBytes = base64UrlToUint8Array(encodedSignature);
  const isValid = await crypto.subtle.verify(
    "RSASSA-PKCS1-v1_5",
    cryptoKey,
    signatureBytes,
    signedContent
  );

  if (!isValid) {
    throw new Error("Invalid JWT signature");
  }

  return payload.sub.trim();
}

function getServiceAccountCredentials(env) {
  const rawJson = env?.FIREBASE_SERVICE_ACCOUNT_JSON;

  if (typeof rawJson === "string" && rawJson.trim().length > 0) {
    const parsed = JSON.parse(rawJson);
    if (!parsed.client_email || !parsed.private_key) {
      throw new Error("FIREBASE_SERVICE_ACCOUNT_JSON is missing client_email or private_key");
    }
    return {
      projectId: env?.FIREBASE_PROJECT_ID || parsed.project_id || DEFAULT_PROJECT_ID,
      clientEmail: parsed.client_email,
      privateKey: parsed.private_key,
    };
  }

  throw new Error(
    "Missing service account credentials. Set FIREBASE_SERVICE_ACCOUNT_JSON in Worker secrets."
  );
}

async function getGoogleAccessToken(clientEmail, privateKey) {
  const now = Date.now();
  if (
    cachedAccessToken.token &&
    cachedAccessToken.cacheKey === clientEmail &&
    now < cachedAccessToken.expiresAtMs - 60_000
  ) {
    return cachedAccessToken.token;
  }

  const nowSec = Math.floor(now / 1000);
  const headerB64 = stringToBase64Url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const payloadB64 = stringToBase64Url(
    JSON.stringify({
      iss: clientEmail,
      sub: clientEmail,
      aud: GOOGLE_OAUTH_TOKEN_URL,
      scope: OAUTH_SCOPES,
      iat: nowSec,
      exp: nowSec + 3600,
    })
  );

  const signingInput = `${headerB64}.${payloadB64}`;
  const keyBuffer = pemToArrayBuffer(privateKey);
  const cryptoKey = await crypto.subtle.importKey(
    "pkcs8",
    keyBuffer,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  );

  const sigBuffer = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    cryptoKey,
    new TextEncoder().encode(signingInput)
  );
  const assertion = `${signingInput}.${uint8ArrayToBase64Url(new Uint8Array(sigBuffer))}`;

  const res = await fetch(GOOGLE_OAUTH_TOKEN_URL, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }).toString(),
  });

  if (!res.ok) {
    throw new Error(`Failed to exchange service account JWT for access token: HTTP ${res.status}`);
  }

  const tokenData = await res.json();
  const expiresInSec = typeof tokenData.expires_in === "number" ? tokenData.expires_in : 3600;
  cachedAccessToken = {
    token: tokenData.access_token,
    expiresAtMs: now + expiresInSec * 1000,
    cacheKey: clientEmail,
  };
  return cachedAccessToken.token;
}

/**
 * Converts a Firestore REST Value object into a plain JavaScript value.
 */
export function parseFirestoreValue(valueObj) {
  if (!valueObj || typeof valueObj !== "object") return undefined;
  if ("nullValue" in valueObj) return null;
  if ("stringValue" in valueObj) return valueObj.stringValue;
  if ("booleanValue" in valueObj) return Boolean(valueObj.booleanValue);
  if ("integerValue" in valueObj) return Number(valueObj.integerValue);
  if ("doubleValue" in valueObj) return Number(valueObj.doubleValue);
  if ("timestampValue" in valueObj) return valueObj.timestampValue;
  if ("arrayValue" in valueObj) {
    const values = valueObj.arrayValue?.values;
    return Array.isArray(values) ? values.map(parseFirestoreValue) : [];
  }
  if ("mapValue" in valueObj) {
    return parseFirestoreFields(valueObj.mapValue?.fields);
  }
  return undefined;
}

export function parseFirestoreFields(fields) {
  if (!fields || typeof fields !== "object") return {};
  const result = {};
  for (const [key, val] of Object.entries(fields)) {
    result[key] = parseFirestoreValue(val);
  }
  return result;
}

async function getFirestoreDocument(projectId, accessToken, docPath) {
  const docWithMeta = await getFirestoreDocumentWithMeta(projectId, accessToken, docPath);
  return docWithMeta ? docWithMeta.fields : null;
}

async function getFirestoreDocumentWithMeta(projectId, accessToken, docPath) {
  const url = `https://firestore.googleapis.com/v1/projects/${encodeURIComponent(
    projectId
  )}/databases/(default)/documents/${docPath}`;
  const res = await fetch(url, {
    method: "GET",
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  if (res.status === 404) {
    return null;
  }
  if (!res.ok) {
    throw new Error(`Firestore GET ${docPath} failed: HTTP ${res.status}`);
  }
  const doc = await res.json();
  return {
    fields: parseFirestoreFields(doc.fields),
    createTime: typeof doc.createTime === "string" ? doc.createTime : "",
    updateTime: typeof doc.updateTime === "string" ? doc.updateTime : "",
  };
}

function parseTimestampMs(rawTimestamp, fallbackIso = "") {
  if (typeof rawTimestamp === "number" && Number.isFinite(rawTimestamp)) {
    return rawTimestamp < 1e12 ? rawTimestamp * 1000 : rawTimestamp;
  }
  if (typeof rawTimestamp === "string" && rawTimestamp.trim().length > 0) {
    const trimmed = rawTimestamp.trim();
    if (/^\d+$/.test(trimmed)) {
      const num = Number(trimmed);
      return num < 1e12 ? num * 1000 : num;
    }
    const parsed = Date.parse(trimmed);
    if (!Number.isNaN(parsed)) {
      return parsed;
    }
  }
  if (typeof fallbackIso === "string" && fallbackIso.trim().length > 0) {
    const parsedFallback = Date.parse(fallbackIso.trim());
    if (!Number.isNaN(parsedFallback)) {
      return parsedFallback;
    }
  }
  return NaN;
}

/**
 * Claims `pushedAt` on `chats/{chatId}/messages/{messageId}` using the
 * `currentDocument.updateTime` precondition BEFORE sending any FCM requests.
 * Returns `true` if claimed, or `false` if already claimed / precondition failed.
 */
async function claimMessagePushedAt(
  projectId,
  accessToken,
  chatId,
  messageId,
  updateTime,
  isoTimestamp
) {
  if (typeof updateTime !== "string" || updateTime.trim().length === 0) {
    return false;
  }
  const docPath = `chats/${encodeURIComponent(chatId)}/messages/${encodeURIComponent(messageId)}`;
  const query = new URLSearchParams({
    "updateMask.fieldPaths": "pushedAt",
    "currentDocument.updateTime": updateTime,
  });
  const url = `https://firestore.googleapis.com/v1/projects/${encodeURIComponent(
    projectId
  )}/databases/(default)/documents/${docPath}?${query.toString()}`;
  const res = await fetch(url, {
    method: "PATCH",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      fields: {
        pushedAt: { timestampValue: isoTimestamp },
      },
    }),
  });
  if (res.status === 400 || res.status === 409 || res.status === 412) {
    return false;
  }
  if (!res.ok) {
    throw new Error(`Firestore PATCH pushedAt on ${docPath} failed: HTTP ${res.status}`);
  }
  return true;
}

async function removeInvalidFcmTokens(projectId, accessToken, recipientUid, invalidTokens) {
  if (!Array.isArray(invalidTokens) || invalidTokens.length === 0) return;
  const commitUrl = `https://firestore.googleapis.com/v1/projects/${encodeURIComponent(
    projectId
  )}/databases/(default)/documents:commit`;
  const documentName = `projects/${projectId}/databases/(default)/documents/userPrivate/${recipientUid}`;

  const res = await fetch(commitUrl, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      writes: [
        {
          transform: {
            document: documentName,
            fieldTransforms: [
              {
                fieldPath: "fcmTokens",
                removeAllFromArray: {
                  values: invalidTokens.map((t) => ({ stringValue: t })),
                },
              },
            ],
          },
        },
      ],
    }),
  });

  if (!res.ok) {
    console.error(`Failed to remove invalid tokens for ${recipientUid}: HTTP ${res.status}`);
  }
}

function isUnregisteredOrInvalidFcmError(status, errorPayload) {
  const err = errorPayload?.error;
  if (!err) return false;

  const details = Array.isArray(err.details) ? err.details : [];
  for (const detail of details) {
    if (
      detail?.errorCode === "UNREGISTERED" ||
      detail?.errorCode === "INVALID_ARGUMENT"
    ) {
      return true;
    }
  }

  if (status === 404 || err.status === "NOT_FOUND") {
    return true;
  }

  const msg = typeof err.message === "string" ? err.message.toLowerCase() : "";
  if (
    msg.includes("not a valid fcm registration token") ||
    msg.includes("registration-token-not-registered") ||
    msg.includes("invalid-registration-token") ||
    msg.includes("unregistered")
  ) {
    return true;
  }

  return false;
}

/**
 * Formats senderName and notification body using the exact rules from `functions/index.js`.
 */
export function buildNotificationContent(chat, message, senderId) {
  const participantInfo =
    chat.participantInfo && typeof chat.participantInfo === "object"
      ? chat.participantInfo
      : {};
  const senderInfo =
    senderId && participantInfo[senderId] && typeof participantInfo[senderId] === "object"
      ? participantInfo[senderId]
      : {};
  const displayName =
    typeof senderInfo.displayName === "string" ? senderInfo.displayName.trim() : "";
  const username =
    typeof senderInfo.username === "string" ? senderInfo.username.trim() : "";

  let senderName = "New message";
  if (displayName.length > 0) {
    senderName = displayName;
  } else if (username.length > 0) {
    senderName = `@${username}`;
  }

  const rawType =
    typeof message.messageType === "string" ? message.messageType.trim() : "";
  const messageType = rawType.length > 0 ? rawType : "text";
  const hasCiphertext =
    typeof message.ciphertext === "string" && message.ciphertext.trim().length > 0;

  let body = "";
  if (messageType === "text") {
    if (hasCiphertext) {
      body = "New message";
    } else {
      const text = typeof message.text === "string" ? message.text : "";
      body = text.length > 100 ? `${text.slice(0, 100)}...` : text;
    }
  } else if (messageType === "image" || messageType === "photo") {
    body = "Photo";
  } else if (messageType === "video") {
    body = "Video";
  } else if (messageType === "voice" || messageType === "audio") {
    body = "Voice message";
  } else if (messageType === "document" || messageType === "file") {
    body = "Document";
  } else if (messageType === "location") {
    body = "Location";
  } else if (messageType === "contact") {
    body = "Contact";
  } else if (hasCiphertext) {
    body = "New message";
  } else {
    const text = typeof message.text === "string" ? message.text : "";
    body = text.length > 100 ? `${text.slice(0, 100)}...` : text;
  }

  return { senderName, body };
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") {
      return jsonResponse({ ok: true }, 204);
    }

    const url = new URL(request.url);
    if (url.pathname !== "/notify") {
      return jsonResponse({ error: "Not found" }, 404);
    }

    if (request.method !== "POST") {
      return jsonResponse({ error: "Method not allowed" }, 405);
    }

    let creds;
    try {
      creds = getServiceAccountCredentials(env);
    } catch {
      console.error("Worker configuration error: invalid service account secret");
      return jsonResponse({ error: "Server configuration error" }, 500);
    }

    const projectId = creds.projectId;

    // 1. Verify caller's Firebase ID token
    const authHeader = request.headers.get("Authorization") || "";
    const match = authHeader.match(/^Bearer\s+(.+)$/i);
    if (!match) {
      return jsonResponse({ error: "Missing or invalid Authorization header" }, 401);
    }

    let callerUid;
    try {
      callerUid = await verifyFirebaseIdToken(match[1], projectId);
    } catch {
      console.warn("Firebase ID token verification failed");
      return jsonResponse({ error: "Invalid ID token" }, 401);
    }

    // 2. Parse request body
    let payload;
    try {
      payload = await request.json();
    } catch {
      return jsonResponse({ error: "Invalid JSON body" }, 400);
    }

    const chatId = typeof payload?.chatId === "string" ? payload.chatId.trim() : "";
    const messageId = typeof payload?.messageId === "string" ? payload.messageId.trim() : "";
    if (!chatId || !messageId || chatId.includes("/") || messageId.includes("/")) {
      return jsonResponse({ error: "Valid chatId and messageId are required" }, 400);
    }

    try {
      const accessToken = await getGoogleAccessToken(creds.clientEmail, creds.privateKey);

      // 3. Read chats/{chatId}/messages/{messageId} (with updateTime metadata)
      const messagePath = `chats/${encodeURIComponent(chatId)}/messages/${encodeURIComponent(messageId)}`;
      const messageDoc = await getFirestoreDocumentWithMeta(projectId, accessToken, messagePath);
      if (!messageDoc) {
        return jsonResponse({ error: "Message not found" }, 404);
      }

      const message = messageDoc.fields;
      const messageUpdateTime = messageDoc.updateTime;

      // 4. Read chats/{chatId} and verify caller is in chat.participantIds
      const chatPath = `chats/${encodeURIComponent(chatId)}`;
      const chat = await getFirestoreDocument(projectId, accessToken, chatPath);
      if (!chat) {
        return jsonResponse({ error: "Chat not found" }, 404);
      }

      const participantIds = Array.isArray(chat.participantIds) ? chat.participantIds : [];
      if (!participantIds.includes(callerUid)) {
        return jsonResponse({ error: "Forbidden: caller is not in chat.participantIds" }, 403);
      }

      const senderId = typeof message.senderId === "string" ? message.senderId.trim() : "";
      if (!senderId || senderId !== callerUid) {
        return jsonResponse({ error: "Forbidden: caller is not the message sender" }, 403);
      }

      // Skip call logs (exact match with functions/index.js)
      const rawType =
        typeof message.messageType === "string" ? message.messageType.trim() : "";
      if (rawType === "call") {
        return jsonResponse({ skipped: "call_message" }, 200);
      }

      // Verify message timestamp is within the last 180 s
      const messageTimeMs = parseTimestampMs(message.timestamp, messageDoc.createTime);
      const ageSeconds = (Date.now() - messageTimeMs) / 1000;
      if (!Number.isFinite(messageTimeMs) || ageSeconds > MAX_MESSAGE_AGE_SECONDS) {
        return jsonResponse({ skipped: "stale" }, 200);
      }

      // If chat.blockedBy exists and is not empty, stop
      if (Array.isArray(chat.blockedBy) && chat.blockedBy.length > 0) {
        return jsonResponse({ skipped: "blocked" }, 200);
      }

      // If pushedAt is already set, return 200 {skipped:"duplicate"}
      if (message.pushedAt) {
        return jsonResponse({ skipped: "duplicate" }, 200);
      }

      const recipients = participantIds.filter(
        (uid) => typeof uid === "string" && uid.trim().length > 0 && uid !== senderId
      );
      if (recipients.length === 0) {
        return jsonResponse({ skipped: "no_recipients" }, 200);
      }

      // 5. Claim pushedAt with currentDocument.updateTime precondition BEFORE sending FCM
      const claimed = await claimMessagePushedAt(
        projectId,
        accessToken,
        chatId,
        messageId,
        messageUpdateTime,
        new Date().toISOString()
      );
      if (!claimed) {
        return jsonResponse({ skipped: "duplicate" }, 200);
      }

      const { senderName, body } = buildNotificationContent(chat, message, senderId);
      const fcmSendUrl = `https://fcm.googleapis.com/v1/projects/${encodeURIComponent(
        projectId
      )}/messages:send`;

      let totalSent = 0;
      let totalFailed = 0;

      for (const recipientUid of recipients) {
        try {
          const userDoc = await getFirestoreDocument(
            projectId,
            accessToken,
            `userPrivate/${encodeURIComponent(recipientUid)}`
          );
          if (!userDoc) {
            continue;
          }

          const fcmTokens = Array.isArray(userDoc.fcmTokens)
            ? [
                ...new Set(
                  userDoc.fcmTokens.filter(
                    (t) => typeof t === "string" && t.trim().length > 0
                  )
                ),
              ]
            : [];

          if (fcmTokens.length === 0) {
            continue;
          }

          const invalidTokens = [];

          for (const token of fcmTokens) {
            const fcmRes = await fetch(fcmSendUrl, {
              method: "POST",
              headers: {
                Authorization: `Bearer ${accessToken}`,
                "Content-Type": "application/json",
              },
              body: JSON.stringify({
                message: {
                  token,
                  notification: {
                    title: senderName,
                    body,
                  },
                  data: {
                    type: "chat",
                    chatId: String(chatId),
                    senderId: String(senderId),
                  },
                  android: {
                    priority: "HIGH",
                    notification: {
                      channel_id: "chat_messages",
                      title: senderName,
                      body,
                      tag: String(chatId),
                    },
                  },
                },
              }),
            });

            if (fcmRes.ok) {
              totalSent += 1;
            } else {
              totalFailed += 1;
              let errJson = null;
              try {
                errJson = await fcmRes.json();
              } catch {
                // ignore JSON parse error
              }
              if (isUnregisteredOrInvalidFcmError(fcmRes.status, errJson)) {
                invalidTokens.push(token);
              }
            }
          }

          console.log(
            `notify chatId=${chatId} recipient=${recipientUid}: sent=${
              fcmTokens.length - invalidTokens.length
            }, invalidRemoved=${invalidTokens.length}`
          );

          if (invalidTokens.length > 0) {
            await removeInvalidFcmTokens(
              projectId,
              accessToken,
              recipientUid,
              invalidTokens
            );
          }
        } catch {
          console.error(
            `Error sending notification to recipient ${recipientUid} for chat ${chatId}`
          );
        }
      }

      console.log(
        `notify completed for chatId=${chatId} messageId=${messageId}: totalSent=${totalSent}, totalFailed=${totalFailed}`
      );

      return jsonResponse({ ok: true, totalSent, totalFailed }, 200);
    } catch {
      console.error(`Error in Worker /notify for chatId=${chatId} messageId=${messageId}`);
      return jsonResponse({ error: "Internal server error" }, 500);
    }
  },
};
