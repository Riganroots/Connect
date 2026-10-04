# User blocking

Signed-in users can block an activity host from the chat header, or a participant
beside their chat message. Profile → Blocked users lists saved blocks and lets the
user unblock. Confirmation is required. Preview Mode has no block controls.

The private `users/{uid}/blockedUsers/{targetUid}` collection synchronizes the
blocklist across devices. Only its owner can read or write it. Self-blocks and
extra privilege fields are rejected. Display names are labels, not identities;
filtering uses Firebase UIDs from activity/chat documents.

Blocking hides that user's hosted activities, availability, and chat messages for
the blocker. An open chat hosted by a blocked user shows an unblock/back screen.
The app rejects sending into a blocked host's activity. Unblocking restores
visibility. Blocking does not leave joined activities, remove historical messages,
change shared activity membership, or submit a report. Public activity data stays
readable to signed-in clients; this is a visibility preference, not a private
activity authorization model. Others in a group can still see and send messages.

Server notification sending checks blocks in both directions before sending chat
or join notifications. This needs deployment of the updated Cloud Functions;
updating the APK alone does not change the live server. Already delivered or queued
notifications are not revoked. A block racing an in-flight send can still allow
that send. Existing local notification history is not filtered.

Block writes confirm only after Firestore acknowledges them. A timeout explains
that queued writes may complete after reconnection. A sync failure is displayed
in chat and the blocklist manager; without a cached blocklist, a failed initial
sync may leave content visible until sync is restored.

Chat sender UID is stored only on the in-memory cloud model via Room's Ignore
annotation, preserving the preview chat table schema and avoiding a database
migration for this change.

## Verification

- 19 Firestore emulator tests passed, covering private blocklists, foreign writes,
  self-blocks, invalid fields and unblocking, alongside prior access/report tests.
- 5 Node tests passed for notification suppression in either direction, normal
  delivery, missing identity, self notifications and lookup failure.
- Android CI must compile the Room model and UI changes before merging.
- On two devices, block a host/participant and verify discovery, availability,
  existing chat, restart, second-device sync, unblock and account switching.
- Deploy rules/functions to staging after checking live configuration, then verify
  notification suppression and offline/timeout behavior. No live deployment has
  been made by this change.
