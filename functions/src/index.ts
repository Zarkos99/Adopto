/**
 * Import function triggers from their respective submodules:
 *
 * import {onCall} from "firebase-functions/v2/https";
 * import {onDocumentWritten} from "firebase-functions/v2/firestore";
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

// Start writing functions
// https://firebase.google.com/docs/functions/typescript

import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import {applicationDefault} from "firebase-admin/app";

admin.initializeApp({
  credential: applicationDefault(),
  projectId: "adopto-61044",
});

export const sendMessageNotification = functions.firestore
  .document("Chats/{chatId}/Messages/{messageId}")
  .onCreate(async (snap, context) => {
    const messageData = snap.data();
    if (!messageData) return;

    const receiverId = messageData.receiver_id;
    const messageText = messageData.content || "You have a new message";

    try {
      const userDoc =
          await admin.firestore().collection("Users").doc(receiverId).get();
      const fcmToken = userDoc.data()?.fcm_token;

      if (!fcmToken) {
        console.log("No FCM token found for user:", receiverId);
        return;
      }

      console.log("Sending FCM to:", receiverId);
      console.log("Token:", fcmToken);
      await admin.messaging().send({
        token: fcmToken,
        data: {
          chat_id: String(context.params.chatId),
          sender_id: String(messageData.sender_id),
          message_id: String(messageData.message_id),
          content: String(messageData.content),
        },
        notification: {
          title: "New Message",
          body: messageText,
        },
      });
    } catch (error) {
      console.error("Error sending message notification:", error);
    }
  });

