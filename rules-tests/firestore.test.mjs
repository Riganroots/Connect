import { readFileSync } from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, collection, getDoc, getDocs, setDoc, updateDoc, deleteDoc, writeBatch,
  serverTimestamp, Timestamp, query, orderBy, limitToLast } from 'firebase/firestore';

let env;
before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-connect-rules',
    firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') }
  });
});
beforeEach(async () => { await env.clearFirestore(); });
after(async () => { await env?.cleanup(); });
const dbFor = uid => env.authenticatedContext(uid).firestore();
const activity = (overrides = {}) => ({
  organizerId: 'host', organizerName: 'Host', organizerRating: 0,
  title: 'Saturday walk', category: 'Explore', location: 'Kathmandu',
  date: '2026-10-10', time: '08:00', pricePerPerson: 'Free', participantsNeeded: 4,
  description: 'Walk together', joinedCount: 1, isVerifiedOrganizer: false,
  createdAt: serverTimestamp(), updatedAt: serverTimestamp(), ...overrides
});
const member = () => ({ activityId: 'walk', joinedAt: serverTimestamp() });
const profile = uid => ({ uid, displayName: 'Member', location: '', bio: '',
  interests: [], photoUrl: '', isVerified: false, schemaVersion: 1,
  createdAt: serverTimestamp(), updatedAt: serverTimestamp(), lastLoginAt: serverTimestamp() });
const message = uid => ({ senderId: uid, senderName: 'Member', text: 'Hello', createdAt: serverTimestamp() });
async function createWalk(overrides = {}) {
  const db = dbFor('host');
  const batch = writeBatch(db);
  batch.set(doc(db, 'activities/walk'), activity(overrides));
  batch.set(doc(db, 'users/host/joinedActivities/walk'), member());
  return batch.commit();
}
async function join(uid, count = 2) {
  const db = dbFor(uid); const batch = writeBatch(db);
  batch.set(doc(db, `users/${uid}/joinedActivities/walk`), member());
  batch.update(doc(db, 'activities/walk'), { joinedCount: count, updatedAt: serverTimestamp() });
  return batch.commit();
}

test('host creates activity and membership atomically; signed-in discovery query works', async () => {
  await assertSucceeds(createWalk());
  const db = dbFor('guest');
  await assertSucceeds(getDocs(query(collection(db, 'activities'), orderBy('createdAt', 'desc'))));
});
test('anonymous users cannot discover, read profiles, or write', async () => {
  await createWalk(); const db = env.unauthenticatedContext().firestore();
  await assertFails(getDocs(collection(db, 'activities')));
  await assertFails(getDoc(doc(db, 'users/host')));
  await assertFails(setDoc(doc(db, 'availability/guest'), {}));
});
test('activity alone and spoofed host are rejected', async () => {
  const db = dbFor('guest');
  await assertFails(setDoc(doc(db, 'activities/walk'), activity()));
  await assertFails(createWalk({ organizerId: 'other' }));
});
test('client cannot claim verified status, rating, or extra activity privileges', async () => {
  await assertFails(createWalk({ isVerifiedOrganizer: true }));
  await assertFails(createWalk({ organizerRating: 5 }));
  await assertFails(createWalk({ admin: true }));
});
test('join and leave update both membership and count', async () => {
  await createWalk(); await assertSucceeds(join('guest'));
  const db = dbFor('guest'); const batch = writeBatch(db);
  batch.delete(doc(db, 'users/guest/joinedActivities/walk'));
  batch.update(doc(db, 'activities/walk'), { joinedCount: 1, updatedAt: serverTimestamp() });
  await assertSucceeds(batch.commit());
  await assertFails(setDoc(doc(db, 'activities/walk/messages/after-leaving'), message('guest')));
});
test('membership-only join, count-only change, and inflated count are rejected', async () => {
  await createWalk(); const db = dbFor('guest');
  await assertFails(setDoc(doc(db, 'users/guest/joinedActivities/walk'), member()));
  await assertFails(updateDoc(doc(db, 'activities/walk'), { joinedCount: 2, updatedAt: serverTimestamp() }));
  await assertFails(join('guest', 100));
});
test('membership-only leave, foreign membership, and host leaving are rejected', async () => {
  await createWalk(); await join('guest'); const db = dbFor('guest');
  await assertFails(deleteDoc(doc(db, 'users/guest/joinedActivities/walk')));
  await assertFails(deleteDoc(doc(db, 'users/host/joinedActivities/walk')));
  await assertFails(deleteDoc(doc(dbFor('host'), 'users/host/joinedActivities/walk')));
});
test('join cannot edit activity title or organizer', async () => {
  await createWalk(); const db = dbFor('guest'); const batch = writeBatch(db);
  batch.set(doc(db, 'users/guest/joinedActivities/walk'), member());
  batch.update(doc(db, 'activities/walk'), { joinedCount: 2, updatedAt: serverTimestamp(), title: 'Hijacked' });
  await assertFails(batch.commit());
});
test('chat is member-only and supports the app message query', async () => {
  await createWalk(); const host = dbFor('host'), guest = dbFor('guest');
  await assertSucceeds(setDoc(doc(host, 'activities/walk/messages/hello'), message('host')));
  await assertFails(getDocs(collection(guest, 'activities/walk/messages')));
  await assertFails(setDoc(doc(guest, 'activities/walk/messages/outsider'), message('guest')));
  await join('guest');
  await assertSucceeds(getDocs(query(collection(guest, 'activities/walk/messages'), orderBy('createdAt'), limitToLast(200))));
  await assertSucceeds(setDoc(doc(guest, 'activities/walk/messages/reply'), message('guest')));
});
test('chat rejects sender spoofing, oversize text, editing and deletion', async () => {
  await createWalk(); const db = dbFor('host'), ref = doc(db, 'activities/walk/messages/hello');
  await assertFails(setDoc(ref, message('guest')));
  await assertFails(setDoc(ref, { ...message('host'), text: 'x'.repeat(1001) }));
  await setDoc(ref, message('host'));
  await assertFails(updateDoc(ref, { text: 'Changed' }));
  await assertFails(deleteDoc(ref));
});
test('profile initialization and login refresh work, foreign access and self verification fail', async () => {
  const db = dbFor('guest'), ref = doc(db, 'users/guest');
  await assertSucceeds(setDoc(ref, profile('guest')));
  await assertSucceeds(updateDoc(ref, { updatedAt: serverTimestamp(), lastLoginAt: serverTimestamp() }));
  await assertFails(getDoc(doc(dbFor('host'), 'users/guest')));
  await assertFails(setDoc(doc(dbFor('host'), 'users/guest'), profile('guest')));
  await assertFails(updateDoc(ref, { isVerified: true, updatedAt: serverTimestamp(), lastLoginAt: serverTimestamp() }));
  await assertFails(updateDoc(ref, { admin: true, updatedAt: serverTimestamp(), lastLoginAt: serverTimestamp() }));
});
test('private device tokens and personal selections remain owner-only', async () => {
  await createWalk(); const db = dbFor('guest');
  const token = doc(db, 'users/guest/devices/token');
  await assertSucceeds(setDoc(token, { token: 'push-token', platform: 'android', appVersion: '0.2', updatedAt: serverTimestamp() }));
  await assertFails(getDoc(doc(dbFor('host'), 'users/guest/devices/token')));
  await assertFails(setDoc(doc(dbFor('host'), 'users/guest/devices/foreign'), { token: 'stolen' }));
  await assertSucceeds(setDoc(doc(db, 'users/guest/savedActivities/walk'), { activityId: 'walk', savedAt: serverTimestamp() }));
  await assertFails(getDocs(collection(dbFor('host'), 'users/guest/savedActivities')));
  await assertSucceeds(setDoc(doc(db, 'users/guest/groupMemberships/hiking'), { groupId: 'hiking', joinedAt: serverTimestamp() }));
  await assertFails(setDoc(doc(db, 'users/guest/savedActivities/missing'), { activityId: 'missing', savedAt: serverTimestamp() }));
});
test('availability writes enforce identity, expiry and verification', async () => {
  const db = dbFor('guest'), ref = doc(db, 'availability/guest');
  const data = { userId: 'guest', userName: 'Member', statusText: 'Coffee', iconType: 'coffee',
    isVerified: false, updatedAt: serverTimestamp(), expiresAt: Timestamp.fromMillis(Date.now() + 7200000) };
  await assertSucceeds(setDoc(ref, data));
  await assertSucceeds(getDocs(query(collection(dbFor('host'), 'availability'), orderBy('updatedAt', 'desc'))));
  await assertFails(setDoc(ref, { ...data, isVerified: true }));
  await assertFails(setDoc(ref, { ...data, expiresAt: Timestamp.fromMillis(Date.now() + 86400000) }));
  await assertFails(setDoc(doc(dbFor('host'), 'availability/guest'), data));
});
test('unknown collections and unsupported client deletions default to denied', async () => {
  await createWalk(); const db = dbFor('host');
  await assertFails(setDoc(doc(db, 'admin/settings'), { enabled: true }));
  await assertFails(deleteDoc(doc(db, 'activities/walk')));
  await assertFails(deleteDoc(doc(db, 'users/host')));
});

test('activity member submits private report; clients cannot retrieve or alter it', async () => {
  await createWalk(); await join('guest'); const db = dbFor('guest');
  const ref = doc(db, 'reports/complaint');
  const data = { reporterId: 'guest', activityId: 'walk', reportedUserId: 'host',
    reason: 'Unsafe activity', details: 'Unsafe meeting location', createdAt: serverTimestamp() };
  await assertSucceeds(setDoc(ref, data));
  await assertFails(getDoc(ref));
  await assertFails(getDoc(doc(dbFor('host'), 'reports/complaint')));
  await assertFails(getDocs(collection(db, 'reports')));
  await assertFails(updateDoc(ref, { reason: 'Other' }));
  await assertFails(deleteDoc(ref));
});
test('report rejects spoofed identities, moderator fields, and invalid detail', async () => {
  await createWalk(); await join('guest'); const db = dbFor('guest');
  const ref = doc(db, 'reports/complaint');
  const data = { reporterId: 'guest', activityId: 'walk', reportedUserId: 'host',
    reason: 'Spam or scam', details: '', createdAt: serverTimestamp() };
  for (const overrides of [{ reporterId: 'host' }, { reportedUserId: 'stranger' },
    { reason: 'invalid' }, { details: 'x'.repeat(1001) }, { status: 'approved' }]) {
    await assertFails(setDoc(ref, { ...data, ...overrides }));
  }
});
test('outsiders and hosts cannot report through member reporting flow', async () => {
  await createWalk();
  const data = { reporterId: 'guest', activityId: 'walk', reportedUserId: 'host',
    reason: 'Other', details: '', createdAt: serverTimestamp() };
  await assertFails(setDoc(doc(dbFor('guest'), 'reports/outsider'), data));
  await assertFails(setDoc(doc(dbFor('host'), 'reports/self'), { ...data, reporterId: 'host' }));
});
