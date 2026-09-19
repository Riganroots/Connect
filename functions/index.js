"use strict";

const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const {
  onDocumentCreated,
} = require("firebase-functions/v2/firestore");
const { setGlobalOptions } = require("firebase-functions/v2/options");

initializeApp();

setGlobalOptions({
  region: "asia-south1",
  maxInstances: 10,
});

const db = getFirestore();
const messaging = getMessaging();

const MAX_MULTICAST_TOKENS = 500;
const INVALID_TOKEN_CODES = new Set([
  "messaging/invalid-registration-token",
  "messaging/registration-token-not-registered",
]);

async function deviceRecordsForUser(userId) {
  const snapshot = await db
    .collection("users")
    .doc(userId)
    .collection("devices")
    .get();

  return snapshot.docs
    .map((doc) => ({
      ref: doc.ref,
      token: doc.get("token"),
    }))
    .filter((record) => typeof record.token === "string" && record.token.length > 0);
}

function chunks(items, size) {
  const result = [];
  for (let index = 0; index < items.length; index += size) {
    result.push(items.slice(index, index + size));
  }
  return result;
}

async function sendToUser(userId, data) {
  const devices = await deviceRecordsForUser(userId);
  if (devices.length === 0) return;

  for (const group of chunks(devices, MAX_MULTICAST_TOKENS)) {
    const response = await messaging.sendEachForMulticast({
      tokens: group.map((device) => device.token),
      data: {
        targetUserId: userId,
        title: String(data.title || "Connect").slice(0, 120),
        body: String(data.body || "").slice(0, 500),
        type: String(data.type || "general"),
        targetId: String(data.targetId || ""),
      },
      android: {
        priority: "high",
        ttl: 60 * 60 * 1000,
      },
    });

    const removals = [];
    response.responses.forEach((item, index) => {
      if (
        !item.success &&
        item.error &&
        INVALID_TOKEN_CODES.has(item.error.code)
      ) {
        removals.push(group[index].ref.delete());
      }
    });

    await Promise.allSettled(removals);
  }
}

async function activityMemberIds(activityId) {
  const snapshot = await db
    .collectionGroup("joinedActivities")
    .where("activityId", "==", activityId)
    .get();

  const ids = new Set();

  snapshot.docs.forEach((doc) => {
    const userDoc = doc.ref.parent.parent;
    if (userDoc && userDoc.id) {
      ids.add(userDoc.id);
    }
  });

  return ids;
}

exports.notifyActivityChatMessage = onDocumentCreated(
  "activities/{activityId}/messages/{messageId}",
  async (event) => {
    const message = event.data;
    if (!message) return;

    const activityId = event.params.activityId;
    const data = message.data() || {};
    const senderId = typeof data.senderId === "string" ? data.senderId : "";
    const senderName =
      typeof data.senderName === "string" && data.senderName.trim()
        ? data.senderName.trim()
        : "Connect member";
    const messageText =
      typeof data.text === "string" ? data.text.trim() : "";

    if (!senderId || !messageText) return;

    const activitySnapshot = await db
      .collection("activities")
      .doc(activityId)
      .get();

    if (!activitySnapshot.exists) return;

    const activityTitle =
      activitySnapshot.get("title") || "your activity";

    const members = await activityMemberIds(activityId);
    members.delete(senderId);

    const tasks = Array.from(members).map((userId) =>
      sendToUser(userId, {
        title: senderName,
        body: messageText,
        type: "activity_chat",
        targetId: activityId,
        activityTitle,
      })
    );

    await Promise.allSettled(tasks);
  }
);

exports.notifyHostWhenActivityJoined = onDocumentCreated(
  "users/{userId}/joinedActivities/{activityId}",
  async (event) => {
    const joiningUserId = event.params.userId;
    const activityId = event.params.activityId;

    const activitySnapshot = await db
      .collection("activities")
      .doc(activityId)
      .get();

    if (!activitySnapshot.exists) return;

    const organizerId = activitySnapshot.get("organizerId");
    if (
      typeof organizerId !== "string" ||
      organizerId.length === 0 ||
      organizerId === joiningUserId
    ) {
      return;
    }

    const activityTitle =
      activitySnapshot.get("title") || "your activity";
    await sendToUser(organizerId, {
      title: "New activity member",
      body: `Someone joined ${activityTitle}.`,
      type: "activity_join",
      targetId: activityId,
    });
  }
);
