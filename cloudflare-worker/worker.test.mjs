import test from "node:test";
import assert from "node:assert/strict";
import worker, {
  __resetWorkerCachesForTests,
  buildNotificationContent,
} from "./worker.js";

const PROJECT_ID = "baagbaan-boi-20";
const KID = "test-key-id-1";

function uint8ArrayToBase64Url(bytes) {
  return Buffer.from(bytes)
    .toString("base64")
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");
}

function arrayBufferToPem(buffer, label) {
  const b64 = Buffer.from(buffer).toString("base64");
  const lines = b64.match(/.{1,64}/g) || [];
  return `-----BEGIN ${label}-----\n${lines.join("\n")}\n-----END ${label}-----`;
}

async function createTestKeySet() {
  const idTokenKeyPair = await crypto.subtle.generateKey(
    {
      name: "RSASSA-PKCS1-v1_5",
      modulusLength: 2048,
      publicExponent: new Uint8Array([1, 0, 1]),
      hash: "SHA-256",
    },
    true,
    ["sign", "verify"]
  );

  const publicJwk = await crypto.subtle.exportKey("jwk", idTokenKeyPair.publicKey);
  publicJwk.kid = KID;
  publicJwk.alg = "RS256";
  publicJwk.use = "sig";

  const saKeyPair = await crypto.subtle.generateKey(
    {
      name: "RSASSA-PKCS1-v1_5",
      modulusLength: 2048,
      publicExponent: new Uint8Array([1, 0, 1]),
      hash: "SHA-256",
    },
    true,
    ["sign", "verify"]
  );
  const pkcs8Der = await crypto.subtle.exportKey("pkcs8", saKeyPair.privateKey);
  const saPrivateKeyPem = arrayBufferToPem(pkcs8Der, "PRIVATE KEY");

  async function signFirebaseIdToken({
    uid = "alice",
    aud = PROJECT_ID,
    iss = `https://securetoken.google.com/${PROJECT_ID}`,
    expOffsetSec = 3600,
  } = {}) {
    const nowSec = Math.floor(Date.now() / 1000);
    const header = { alg: "RS256", typ: "JWT", kid: KID };
    const payload = {
      iss,
      aud,
      sub: uid,
      user_id: uid,
      iat: nowSec - 10,
      exp: nowSec + expOffsetSec,
    };
    const encHeader = uint8ArrayToBase64Url(Buffer.from(JSON.stringify(header)));
    const encPayload = uint8ArrayToBase64Url(Buffer.from(JSON.stringify(payload)));
    const signingInput = `${encHeader}.${encPayload}`;
    const sig = await crypto.subtle.sign(
      "RSASSA-PKCS1-v1_5",
      idTokenKeyPair.privateKey,
      new TextEncoder().encode(signingInput)
    );
    return `${signingInput}.${uint8ArrayToBase64Url(new Uint8Array(sig))}`;
  }

  const env = {
    FIREBASE_PROJECT_ID: PROJECT_ID,
    FIREBASE_SERVICE_ACCOUNT_JSON: JSON.stringify({
      project_id: PROJECT_ID,
      client_email: "firebase-adminsdk@baagbaan-boi-20.iam.gserviceaccount.com",
      private_key: saPrivateKeyPem,
    }),
  };

  return { publicJwk, signFirebaseIdToken, env };
}

function toFirestoreValue(val) {
  if (val === null || val === undefined) return { nullValue: null };
  if (typeof val === "string") {
    if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/.test(val)) {
      return { timestampValue: val };
    }
    return { stringValue: val };
  }
  if (typeof val === "boolean") return { booleanValue: val };
  if (typeof val === "number") return { integerValue: String(val) };
  if (Array.isArray(val)) {
    return { arrayValue: { values: val.map(toFirestoreValue) } };
  }
  if (typeof val === "object") {
    const fields = {};
    for (const [k, v] of Object.entries(val)) {
      fields[k] = toFirestoreValue(v);
    }
    return { mapValue: { fields } };
  }
  return { stringValue: String(val) };
}

function toFirestoreDoc(obj, meta = {}) {
  const nowIso = new Date().toISOString();
  return {
    fields: toFirestoreValue(obj).mapValue.fields,
    createTime: meta.createTime || nowIso,
    updateTime: meta.updateTime || "2026-10-18T15:00:00.123456Z",
  };
}

test("buildNotificationContent matches functions/index.js senderName & body rules", () => {
  // 1. displayName present + >100 char text
  const longText = "a".repeat(125);
  const res1 = buildNotificationContent(
    {
      participantInfo: {
        alice: { displayName: "Alice Khan", username: "alice_k" },
      },
    },
    { messageType: "text", text: longText },
    "alice"
  );
  assert.equal(res1.senderName, "Alice Khan");
  assert.equal(res1.body, `${"a".repeat(100)}...`);

  // 2. displayName blank -> @username fallback + attachment labels
  const types = [
    ["image", "Photo"],
    ["video", "Video"],
    ["voice", "Voice message"],
    ["document", "Document"],
    ["location", "Location"],
    ["contact", "Contact"],
  ];
  for (const [messageType, expectedBody] of types) {
    const res = buildNotificationContent(
      {
        participantInfo: {
          alice: { displayName: "   ", username: "alice_k" },
        },
      },
      { messageType, text: "ignored" },
      "alice"
    );
    assert.equal(res.senderName, "@alice_k");
    assert.equal(res.body, expectedBody);
  }

  // 3. No participant info -> "New message"
  const res3 = buildNotificationContent({}, { text: "Hello Bob!" }, "alice");
  assert.equal(res3.senderName, "New message");
  assert.equal(res3.body, "Hello Bob!");

  // 4. Encrypted text message (ciphertext present, no text) -> body is "New message"
  const res4 = buildNotificationContent(
    {
      participantInfo: {
        alice: { displayName: "Alice", username: "alice_k" },
      },
    },
    { messageType: "text", ciphertext: "base64ciphertext==", iv: "base64iv==", v: 1 },
    "alice"
  );
  assert.equal(res4.senderName, "Alice");
  assert.equal(res4.body, "New message");

  // 5. Encrypted message with missing messageType -> body is "New message"
  const res5 = buildNotificationContent(
    {},
    { ciphertext: "base64ciphertext==", iv: "base64iv==", v: 1 },
    "alice"
  );
  assert.equal(res5.body, "New message");
});

test("Worker /notify rejects missing or invalid ID token with 401", async () => {
  __resetWorkerCachesForTests();
  const { env } = await createTestKeySet();

  const resMissing = await worker.fetch(
    new Request("https://push.example.workers.dev/notify", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ chatId: "alice_bob", messageId: "msg1" }),
    }),
    env
  );
  assert.equal(resMissing.status, 401);
});

test("Worker /notify claims pushedAt with currentDocument.updateTime before FCM, sends FCM HTTP v1 push, and removes UNREGISTERED tokens", async () => {
  __resetWorkerCachesForTests();
  const { publicJwk, signFirebaseIdToken, env } = await createTestKeySet();
  const idToken = await signFirebaseIdToken({ uid: "alice" });
  const expectedUpdateTime = "2026-10-18T15:00:00.123456Z";
  const freshTimestamp = new Date().toISOString();

  const callOrder = [];
  const fcmRequests = [];
  const patchRequests = [];
  const commitRequests = [];

  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (input, init = {}) => {
    const url = typeof input === "string" ? input : input.url;
    const method = init.method || "GET";

    if (url.includes("securetoken@system.gserviceaccount.com")) {
      return new Response(JSON.stringify({ keys: [publicJwk] }), {
        status: 200,
        headers: { "Cache-Control": "public, max-age=3600" },
      });
    }

    if (url === "https://oauth2.googleapis.com/token") {
      return new Response(
        JSON.stringify({ access_token: "ya29.mock-access-token", expires_in: 3600 }),
        { status: 200 }
      );
    }

    if (
      url.endsWith("/documents/chats/alice_bob/messages/msg1") &&
      method === "GET"
    ) {
      return new Response(
        JSON.stringify(
          toFirestoreDoc(
            {
              senderId: "alice",
              text: "Assalamu Alaikum Bob!",
              messageType: "text",
              timestamp: freshTimestamp,
            },
            { updateTime: expectedUpdateTime }
          )
        ),
        { status: 200 }
      );
    }

    if (url.endsWith("/documents/chats/alice_bob") && method === "GET") {
      return new Response(
        JSON.stringify(
          toFirestoreDoc({
            participantIds: ["alice", "bob"],
            blockedBy: [],
            participantInfo: {
              alice: { displayName: "Alice", username: "alice" },
              bob: { displayName: "Bob", username: "bob" },
            },
          })
        ),
        { status: 200 }
      );
    }

    if (
      url.includes("/documents/chats/alice_bob/messages/msg1?") &&
      method === "PATCH"
    ) {
      callOrder.push("PATCH_pushedAt");
      patchRequests.push({ url, body: JSON.parse(init.body) });
      return new Response(JSON.stringify({}), { status: 200 });
    }

    if (url.endsWith("/documents/userPrivate/bob") && method === "GET") {
      return new Response(
        JSON.stringify(
          toFirestoreDoc({
            uid: "bob",
            fcmTokens: ["valid_token_1", "dead_token_2"],
          })
        ),
        { status: 200 }
      );
    }

    if (url.endsWith(`/projects/${PROJECT_ID}/messages:send`) && method === "POST") {
      callOrder.push("POST_FCM");
      const parsed = JSON.parse(init.body);
      fcmRequests.push(parsed);
      if (parsed.message.token === "dead_token_2") {
        return new Response(
          JSON.stringify({
            error: {
              code: 404,
              message: "Requested entity was not found.",
              status: "NOT_FOUND",
              details: [{ errorCode: "UNREGISTERED" }],
            },
          }),
          { status: 404 }
        );
      }
      return new Response(JSON.stringify({ name: "projects/baagbaan-boi-20/messages/1" }), {
        status: 200,
      });
    }

    if (url.endsWith("/documents:commit") && method === "POST") {
      commitRequests.push(JSON.parse(init.body));
      return new Response(JSON.stringify({ writeResults: [{}] }), { status: 200 });
    }

    throw new Error(`Unexpected fetch in test: ${method} ${url}`);
  };

  try {
    const response = await worker.fetch(
      new Request("https://push.example.workers.dev/notify", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${idToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ chatId: "alice_bob", messageId: "msg1" }),
      }),
      env
    );

    assert.equal(response.status, 200);
    const body = await response.json();
    assert.deepEqual(body, { ok: true, totalSent: 1, totalFailed: 1 });

    // Verify pushedAt was claimed BEFORE sending FCM and included currentDocument.updateTime
    assert.equal(patchRequests.length, 1);
    assert.equal(callOrder[0], "PATCH_pushedAt");
    assert.ok(
      patchRequests[0].url.includes(
        `currentDocument.updateTime=${encodeURIComponent(expectedUpdateTime)}`
      )
    );
    assert.ok(patchRequests[0].body.fields.pushedAt.timestampValue);

    // Verify FCM HTTP v1 requests
    assert.equal(fcmRequests.length, 2);
    assert.deepEqual(fcmRequests[0], {
      message: {
        token: "valid_token_1",
        notification: {
          title: "Alice",
          body: "Assalamu Alaikum Bob!",
        },
        data: {
          type: "chat",
          chatId: "alice_bob",
          senderId: "alice",
        },
        android: {
          priority: "HIGH",
          notification: {
            channel_id: "chat_messages",
            title: "Alice",
            body: "Assalamu Alaikum Bob!",
            tag: "alice_bob",
          },
        },
      },
    });

    // Verify dead_token_2 was removed via Firestore removeAllFromArray
    assert.equal(commitRequests.length, 1);
    const transform = commitRequests[0].writes[0].transform;
    assert.ok(transform.document.endsWith("/userPrivate/bob"));
    assert.deepEqual(transform.fieldTransforms, [
      {
        fieldPath: "fcmTokens",
        removeAllFromArray: {
          values: [{ stringValue: "dead_token_2" }],
        },
      },
    ]);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("Worker /notify enforces participantIds (403), 180s staleness ({skipped:'stale'}), precondition duplicate ({skipped:'duplicate'}), call skip, and blocked skip", async () => {
  __resetWorkerCachesForTests();
  const { publicJwk, signFirebaseIdToken, env } = await createTestKeySet();
  const aliceToken = await signFirebaseIdToken({ uid: "alice" });
  const malloryToken = await signFirebaseIdToken({ uid: "mallory" });

  let messageDoc = {
    senderId: "alice",
    text: "Voice call",
    messageType: "call",
    timestamp: new Date().toISOString(),
  };
  let chatDoc = {
    participantIds: ["alice", "bob"],
    blockedBy: [],
  };
  let patchStatus = 200;

  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (input, init = {}) => {
    const url = typeof input === "string" ? input : input.url;
    const method = init.method || "GET";

    if (url.includes("securetoken@system.gserviceaccount.com")) {
      return new Response(JSON.stringify({ keys: [publicJwk] }), { status: 200 });
    }
    if (url === "https://oauth2.googleapis.com/token") {
      return new Response(
        JSON.stringify({ access_token: "ya29.mock", expires_in: 3600 }),
        { status: 200 }
      );
    }
    if (url.endsWith("/documents/chats/alice_bob/messages/msg1") && method === "GET") {
      return new Response(JSON.stringify(toFirestoreDoc(messageDoc)), { status: 200 });
    }
    if (url.endsWith("/documents/chats/alice_bob") && method === "GET") {
      return new Response(JSON.stringify(toFirestoreDoc(chatDoc)), { status: 200 });
    }
    if (url.includes("/documents/chats/alice_bob/messages/msg1?") && method === "PATCH") {
      return new Response(JSON.stringify({}), { status: patchStatus });
    }
    throw new Error(`Unexpected fetch: ${method} ${url}`);
  };

  try {
    // 1. Caller not in chat.participantIds -> 403
    const res403NotParticipant = await worker.fetch(
      new Request("https://push.example.workers.dev/notify", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${malloryToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ chatId: "alice_bob", messageId: "msg1" }),
      }),
      env
    );
    assert.equal(res403NotParticipant.status, 403);

    // 2. Caller is in participantIds, or senderId != callerUid, or sender removed from participantIds -> 403
    chatDoc = { participantIds: ["bob"], blockedBy: [] };
    const res403RemovedFromChat = await worker.fetch(
      new Request("https://push.example.workers.dev/notify", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${aliceToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ chatId: "alice_bob", messageId: "msg1" }),
      }),
      env
    );
    assert.equal(res403RemovedFromChat.status, 403);

    // Restore chat participants
    chatDoc = { participantIds: ["alice", "bob"], blockedBy: [] };

    // 3. messageType === "call" -> skipped: "call_message"
    const resCall = await worker.fetch(
      new Request("https://push.example.workers.dev/notify", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${aliceToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ chatId: "alice_bob", messageId: "msg1" }),
      }),
      env
    );
    assert.equal(resCall.status, 200);
    assert.deepEqual(await resCall.json(), { skipped: "call_message" });

    // 4. Stale message (> 180 s old) -> 200 {skipped: "stale"}
    const staleIso = new Date(Date.now() - 181 * 1000).toISOString();
    messageDoc = {
      senderId: "alice",
      text: "Old message",
      messageType: "text",
      timestamp: staleIso,
    };
    const resStale = await worker.fetch(
      new Request("https://push.example.workers.dev/notify", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${aliceToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ chatId: "alice_bob", messageId: "msg1" }),
      }),
      env
    );
    assert.equal(resStale.status, 200);
    assert.deepEqual(await resStale.json(), { skipped: "stale" });

    // 5. pushedAt already set -> 200 {skipped: "duplicate"}
    messageDoc = {
      senderId: "alice",
      text: "Hi",
      messageType: "text",
      timestamp: new Date().toISOString(),
      pushedAt: new Date().toISOString(),
    };
    const resAlready = await worker.fetch(
      new Request("https://push.example.workers.dev/notify", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${aliceToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ chatId: "alice_bob", messageId: "msg1" }),
      }),
      env
    );
    assert.equal(resAlready.status, 200);
    assert.deepEqual(await resAlready.json(), { skipped: "duplicate" });

    // 6. Failed currentDocument.updateTime precondition claim (HTTP 400) -> 200 {skipped: "duplicate"}
    messageDoc = {
      senderId: "alice",
      text: "Hi",
      messageType: "text",
      timestamp: new Date().toISOString(),
    };
    patchStatus = 400;
    const resPreconditionFailed = await worker.fetch(
      new Request("https://push.example.workers.dev/notify", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${aliceToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ chatId: "alice_bob", messageId: "msg1" }),
      }),
      env
    );
    assert.equal(resPreconditionFailed.status, 200);
    assert.deepEqual(await resPreconditionFailed.json(), { skipped: "duplicate" });
    patchStatus = 200;

    // 7. blockedBy non-empty -> 200 {skipped: "blocked"}
    chatDoc = {
      participantIds: ["alice", "bob"],
      blockedBy: ["bob"],
    };
    const resBlocked = await worker.fetch(
      new Request("https://push.example.workers.dev/notify", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${aliceToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ chatId: "alice_bob", messageId: "msg1" }),
      }),
      env
    );
    assert.equal(resBlocked.status, 200);
    assert.deepEqual(await resBlocked.json(), { skipped: "blocked" });
  } finally {
    globalThis.fetch = originalFetch;
  }
});
