"use strict";
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const { initializeApp, deleteApp } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');
const { cleanupAccount } = require('./account-deletion');
let app, db;
before(() => {
  if (!process.env.FIRESTORE_EMULATOR_HOST) throw new Error('Deletion integration tests require the Firestore emulator.');
  app = initializeApp({ projectId: 'demo-connect-rules' }, 'deletion-tests'); db = getFirestore(app);
});
after(async () => { if (app) await deleteApp(app); });
test('cleanup removes nested content and foreign references, preserves others, and retries safely', async () => {
  const uid = 'delete-me';
  const batch = db.batch();
  const set = (path, data) => batch.set(db.doc(path), data);
  set(`accountDeletionJobs/${uid}`, { status: 'queued' });
  set(`users/${uid}`, { uid });
  set(`users/${uid}/devices/phone`, { token: 'private' });
  set(`users/${uid}/blockedUsers/other`, { targetUid: 'other' });
  set(`availability/${uid}`, { userId: uid });
  set('activities/owned', { organizerId: uid, joinedCount: 2 });
  set('activities/owned/messages/foreign', { senderId: 'other', text: 'hosted discussion' });
  set('users/other/joinedActivities/owned', { activityId: 'owned' });
  set('users/other/savedActivities/owned', { activityId: 'owned' });
  set('activities/kept', { organizerId: 'other', joinedCount: 2 });
  set(`users/${uid}/joinedActivities/kept`, { activityId: 'kept' });
  set('activities/kept/messages/other', { senderId: 'other', text: 'keep me' });
  set(`users/other/blockedUsers/${uid}`, { targetUid: uid });
  set('reports/by-deleted', { reporterId: uid, reportedUserId: 'other' });
  set('reports/about-deleted', { reporterId: 'other', reportedUserId: uid });
  set('reports/unrelated', { reporterId: 'other', reportedUserId: 'third' });
  for (let i = 0; i < 127; i++) set(`activities/kept/messages/deleted-${i}`, { senderId: uid });
  await batch.commit();
  let deleted = 0;
  const auth = { updateUser: async (id, data) => { assert.equal(id, uid); assert.equal(data.disabled, true); },
    revokeRefreshTokens: async () => {}, deleteUser: async () => { deleted++; } };
  // Simulate recursive deletion losing its parent before a transient failure.
  const original = db.recursiveDelete.bind(db); let failOnce = true;
  db.recursiveDelete = async ref => {
    if (ref.path === 'activities/owned' && failOnce) {
      failOnce = false; await ref.delete(); throw new Error('partial recursive delete');
    }
    return original(ref);
  };
  await assert.rejects(cleanupAccount(db, auth, uid), /partial recursive delete/);
  assert.equal(deleted, 0);
  assert.equal((await db.doc(`accountDeletionJobs/${uid}/hostedActivities/owned`).get()).exists, true);
  await cleanupAccount(db, auth, uid);
  assert.equal(deleted, 1);
  for (const path of [`users/${uid}`, `users/${uid}/devices/phone`, `availability/${uid}`,
    'activities/owned/messages/foreign', 'users/other/joinedActivities/owned', 'users/other/savedActivities/owned',
    `users/other/blockedUsers/${uid}`, 'reports/by-deleted', 'reports/about-deleted']) {
    assert.equal((await db.doc(path).get()).exists, false, path);
  }
  assert.equal((await db.collectionGroup('messages').where('senderId', '==', uid).get()).empty, true);
  assert.equal((await db.doc('activities/kept').get()).get('joinedCount'), 1);
  assert.equal((await db.doc('activities/kept/messages/other').get()).exists, true);
  assert.equal((await db.doc('reports/unrelated').get()).exists, true);
  await cleanupAccount(db, auth, uid);
  assert.equal((await db.doc('activities/kept').get()).get('joinedCount'), 1);
});
