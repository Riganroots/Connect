"use strict";
const { test } = require("node:test");
const assert = require("node:assert/strict");
const { deletionIdentity, cleanupAccount } = require("./account-deletion");
const request = { auth: { uid: 'own-user', token: { auth_time: 1000 } }, data: {} };
test('deletion derives its UID only from recent verified authentication', () => {
  assert.equal(deletionIdentity(request, 1100), 'own-user');
});
test('anonymous, stale and forged target requests are rejected', () => {
  assert.throws(() => deletionIdentity({}, 1100), { code: 'unauthenticated' });
  assert.throws(() => deletionIdentity(request, 1500), { code: 'failed-precondition' });
  assert.throws(() => deletionIdentity({ ...request, data: { uid: 'victim' } }, 1100), { code: 'invalid-argument' });
  assert.throws(() => deletionIdentity({ ...request, data: { path: 'users/victim' } }, 1100), { code: 'invalid-argument' });
});
test('cleanup failure prevents final Authentication deletion', async () => {
  let deleted = false;
  const auth = { updateUser: async () => {}, revokeRefreshTokens: async () => {}, deleteUser: async () => { deleted = true; } };
  const db = { collection: () => ({ limit: () => { throw new Error('database unavailable'); } }) };
  await assert.rejects(cleanupAccount(db, auth, 'own-user'), /database unavailable/);
  assert.equal(deleted, false);
});
