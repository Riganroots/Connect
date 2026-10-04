"use strict";

// Server-side notification guard. A block in either direction suppresses delivery.
async function notificationBlocked(db, recipientId, sourceUserId) {
  if (!recipientId || !sourceUserId || recipientId === sourceUserId) return true;
  const paths = [
    `accountDeletionJobs/${recipientId}`,
    `accountDeletionJobs/${sourceUserId}`,
    `users/${recipientId}/blockedUsers/${sourceUserId}`,
    `users/${sourceUserId}/blockedUsers/${recipientId}`,
  ];
  const records = await Promise.all(paths.map((path) => db.doc(path).get()));
  return records.some((record) => record.exists);
}

module.exports = { notificationBlocked };
