const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

exports.onMessageCreated = onDocumentCreated(
  "chats/{chatId}/messages/{messageId}",
  async (event) => {
    try {
      const snapshot = event.data;
      if (!snapshot) {
        return;
      }

      const message = snapshot.data();
      if (!message) {
        return;
      }

      // 1. Read the new message. If messageType is "call", stop.
      const rawType = typeof message.messageType === "string"
        ? message.messageType.trim()
        : "";
      if (rawType === "call") {
        return;
      }

      const { chatId } = event.params;
      const senderId = typeof message.senderId === "string"
        ? message.senderId.trim()
        : "";

      // 2. Read chats/{chatId}. Take participantIds and recipients.
      const db = admin.firestore();
      const chatSnap = await db.collection("chats").doc(chatId).get();
      if (!chatSnap.exists) {
        return;
      }

      const chat = chatSnap.data() || {};

      // 3. If chat.blockedBy exists and is not empty, stop.
      if (Array.isArray(chat.blockedBy) && chat.blockedBy.length > 0) {
        return;
      }

      const participantIds = Array.isArray(chat.participantIds)
        ? chat.participantIds
        : [];
      const recipients = participantIds.filter(
        (uid) => typeof uid === "string" && uid.trim().length > 0 && uid !== senderId
      );
      if (recipients.length === 0) {
        return;
      }

      // 5. Sender name: participantInfo[senderId].displayName, or "@" + username, or "New message".
      const participantInfo =
        chat.participantInfo && typeof chat.participantInfo === "object"
          ? chat.participantInfo
          : {};
      const senderInfo =
        senderId && participantInfo[senderId] && typeof participantInfo[senderId] === "object"
          ? participantInfo[senderId]
          : {};
      const displayName =
        typeof senderInfo.displayName === "string"
          ? senderInfo.displayName.trim()
          : "";
      const username =
        typeof senderInfo.username === "string"
          ? senderInfo.username.trim()
          : "";

      let senderName = "New message";
      if (displayName.length > 0) {
        senderName = displayName;
      } else if (username.length > 0) {
        senderName = `@${username}`;
      }

      // 6. Body: for messageType "text" (or missing), use text cut to 100 chars with "..." if longer.
      // For other types use short label: "Photo", "Video", "Voice message", "Document", "Location" or "Contact".
      const messageType = rawType.length > 0 ? rawType : "text";
      let body = "";
      if (messageType === "text") {
        const text = typeof message.text === "string" ? message.text : "";
        body = text.length > 100 ? `${text.slice(0, 100)}...` : text;
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
      } else {
        const text = typeof message.text === "string" ? message.text : "";
        body = text.length > 100 ? `${text.slice(0, 100)}...` : text;
      }

      // 4, 7, 8, 9. Send to each recipient's fcmTokens and clean up invalid tokens.
      let totalSent = 0;
      let totalFailed = 0;

      for (const recipientUid of recipients) {
        try {
          const userRef = db.collection("users").doc(recipientUid);
          const userSnap = await userRef.get();
          if (!userSnap.exists) {
            continue;
          }

          const userData = userSnap.data() || {};
          const fcmTokens = Array.isArray(userData.fcmTokens)
            ? userData.fcmTokens.filter(
                (t) => typeof t === "string" && t.trim().length > 0
              )
            : [];

          if (fcmTokens.length === 0) {
            continue;
          }

          const response = await admin.messaging().sendEachForMulticast({
            tokens: fcmTokens,
            notification: {
              title: senderName,
              body: body,
            },
            data: {
              type: "chat",
              chatId: String(chatId),
              senderId: String(senderId),
            },
            android: {
              priority: "high",
              notification: {
                channelId: "chat_messages",
                title: senderName,
                body: body,
                tag: String(chatId),
              },
            },
          });

          totalSent += response.successCount;
          totalFailed += response.failureCount;

          console.log(
            `onMessageCreated chatId=${chatId} recipient=${recipientUid}: sent=${response.successCount}, failed=${response.failureCount}`
          );

          const invalidTokens = [];
          response.responses.forEach((res, idx) => {
            if (!res.success && res.error) {
              const code = res.error.code;
              if (
                code === "messaging/registration-token-not-registered" ||
                code === "messaging/invalid-registration-token"
              ) {
                invalidTokens.push(fcmTokens[idx]);
              }
            }
          });

          if (invalidTokens.length > 0) {
            await userRef.update({
              fcmTokens: admin.firestore.FieldValue.arrayRemove(...invalidTokens),
            });
          }
        } catch (recipientError) {
          console.error(
            `Error sending notification to recipient ${recipientUid} for chat ${chatId}:`,
            recipientError
          );
        }
      }

      console.log(
        `onMessageCreated completed for chatId=${chatId}: totalSent=${totalSent}, totalFailed=${totalFailed}`
      );
    } catch (error) {
      console.error("Error in onMessageCreated:", error);
    }
  }
);
