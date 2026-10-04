"use strict";

function deletionIdentity(request, nowSeconds = Math.floor(Date.now() / 1000)) {
  if (!request.auth?.uid) {
    const error = new Error("Sign in before deleting your account."); error.code = "unauthenticated"; throw error;
  }
  const authTime = request.auth.token?.auth_time;
  if (!Number.isFinite(authTime) || authTime > nowSeconds + 60 || nowSeconds - authTime > 300) {
    const error = new Error("Confirm your password again before deleting your account."); error.code = "failed-precondition"; throw error;
  }
  if (request.data && Object.keys(request.data).length !== 0) {
    const error = new Error("Deletion accepts no target user or document path."); error.code = "invalid-argument"; throw error;
  }
  return request.auth.uid;
}

async function deleteQuery(db, query) {
  // Re-query after deletion so retries never skip rows or exceed batch limits.
  for (;;) {
    const page = await query.limit(100).get();
    if (page.empty) return;
    const batch = db.batch();
    page.docs.forEach((doc) => batch.delete(doc.ref));
    await batch.commit();
  }
}

async function leaveMembership(db, ref) {
  await db.runTransaction(async (transaction) => {
    const membership = await transaction.get(ref);
    if (!membership.exists) return;
    const activity = db.doc(`activities/${membership.get("activityId")}`);
    const snapshot = await transaction.get(activity);
    if (snapshot.exists && snapshot.get("organizerId") !== ref.parent.parent.id) {
      transaction.update(activity, { joinedCount: Math.max(1, (snapshot.get("joinedCount") || 1) - 1) });
    }
    transaction.delete(ref);
  });
}

async function cleanupAccount(db, auth, uid) {
  // The immutable job document blocks all client access for this UID via rules.
  try {
    await auth.updateUser(uid, { disabled: true });
    await auth.revokeRefreshTokens(uid);
  } catch (error) { if (error.code !== "auth/user-not-found") throw error; }

  const hostedTasks = db.collection(`accountDeletionJobs/${uid}/hostedActivities`);
  async function drainHostedTasks() {
    for (;;) {
      const page = await hostedTasks.limit(25).get();
      if (page.empty) return;
      for (const task of page.docs) {
        const id = task.id;
        await deleteQuery(db, db.collectionGroup("joinedActivities").where("activityId", "==", id));
        await deleteQuery(db, db.collectionGroup("savedActivities").where("activityId", "==", id));
        await db.recursiveDelete(db.doc(`activities/${id}`));
        await task.ref.delete();
      }
    }
  }
  // Keep each activity path until recursive deletion succeeds, including retries
  // after a partially deleted parent document is no longer queryable.
  await drainHostedTasks();
  const hosted = db.collection("activities").where("organizerId", "==", uid);
  for (;;) {
    const page = await hosted.limit(25).get();
    if (page.empty) break;
    for (const activity of page.docs) {
      await hostedTasks.doc(activity.id).set({ activityId: activity.id });
    }
    await drainHostedTasks();
  }
  const memberships = db.collection(`users/${uid}/joinedActivities`);
  for (;;) {
    const page = await memberships.limit(100).get();
    if (page.empty) break;
    for (const membership of page.docs) await leaveMembership(db, membership.ref);
  }
  await deleteQuery(db, db.collectionGroup("messages").where("senderId", "==", uid));
  await deleteQuery(db, db.collectionGroup("blockedUsers").where("targetUid", "==", uid));
  await deleteQuery(db, db.collection("reports").where("reporterId", "==", uid));
  await deleteQuery(db, db.collection("reports").where("reportedUserId", "==", uid));
  await db.doc(`availability/${uid}`).delete();
  await db.recursiveDelete(db.doc(`users/${uid}`));
  // Remove Authentication last, allowing an interrupted job to retry cleanup.
  try { await auth.deleteUser(uid); }
  catch (error) { if (error.code !== "auth/user-not-found") throw error; }
}

module.exports = { deletionIdentity, deleteQuery, leaveMembership, cleanupAccount };
