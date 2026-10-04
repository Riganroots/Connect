"use strict";
const { test } = require("node:test");
const assert = require("node:assert/strict");
const { notificationBlocked } = require("./blocking");
function database(existing = []) {
  return { doc: (path) => ({ get: async () => ({ exists: existing.includes(path) }) }) };
}
test("unblocked members can receive notifications", async () => {
  assert.equal(await notificationBlocked(database(), "recipient", "sender"), false);
});
test("recipient block suppresses sender notifications", async () => {
  assert.equal(await notificationBlocked(database(["users/recipient/blockedUsers/sender"]), "recipient", "sender"), true);
});
test("sender block also suppresses notifications", async () => {
  assert.equal(await notificationBlocked(database(["users/sender/blockedUsers/recipient"]), "recipient", "sender"), true);
});
test("missing identity and self notifications are suppressed", async () => {
  assert.equal(await notificationBlocked(database(), "recipient", ""), true);
  assert.equal(await notificationBlocked(database(), "same", "same"), true);
});
test("lookup failure cannot allow delivery", async () => {
  const db = { doc: () => ({ get: async () => { throw new Error("unavailable"); } }) };
  await assert.rejects(notificationBlocked(db, "recipient", "sender"), /unavailable/);
});
test('pending account deletion suppresses outgoing and incoming notifications', async () => {
  assert.equal(await notificationBlocked(database(['accountDeletionJobs/sender']), 'recipient', 'sender'), true);
  assert.equal(await notificationBlocked(database(['accountDeletionJobs/recipient']), 'recipient', 'sender'), true);
});
