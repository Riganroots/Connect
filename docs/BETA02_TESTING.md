# Connect 0.2.0-beta02 candidate

Application ID: `com.connectapp.npl` (unchanged)
Firebase project configured in the existing client: `connect-dae99`
Candidate version code: `3` (confirm it exceeds the highest Play Console upload
before creating a Play release; Play Console has not been inspected).

## Candidate release notes

- Android Back returns from secondary screens.
- Activity participants can privately report unsafe or misleading activities.
- Block hosts or chat participants; manage unblocking from Profile.
- Password-confirmed account deletion requests retryable server cleanup.
- Firestore permissions and server notification guards have automated tests.

## Build and test

Android CI runs unit tests, builds a debug APK, and validates a release AAB using
an ephemeral CI key. The debug APK is for device checks, not a Play upload.
The CI release key is not the permanent Play upload key. Use the existing manual
Play Internal Testing Bundle workflow and the existing upload identity for a
signed AAB. Never generate a replacement production/upload identity casually.

The existing app/google-services.json matches this package. It does not prove
the live rules, billing, indexes or functions are ready. Deploy and verify the
backend described in ACCOUNT_DELETION.md and USER_BLOCKING.md before claiming
reporting, blocking pushes or deletion work end to end. Firebase rules take effect
for existing installed versions too; compare live configuration before rollout.

## Two-account acceptance checks

Use disposable test accounts on two devices. Record device/Android version,
app version, expected result, actual result and screenshots for failures.

1. Sign in, restart, reset password, sign out and switch accounts. Verify the
   second user cannot see the first user's private profile, saves or chat state.
2. Create, join, save and leave an activity across devices. Verify membership
   count changes once and non-members cannot read or write activity chat.
3. Use system Back from Profile, Create Plan and Chat; dismiss the profile editor.
4. Submit a report as a joined participant; inspect via trusted moderation access.
5. Block a host and a participant, restart, check second-device sync, then unblock.
   Verify blocked content visibility and deployed push suppression.
6. Request deletion with a wrong password, cancel, then use the correct password
   on a disposable account. Verify accepted-request sign-out and eventual complete
   job status, data cleanup and Authentication deletion through trusted admin access.
7. Disconnect/reconnect during chat, report/block changes and deletion requests.
   Verify errors, pending state and no false completed-deletion confirmation.
8. Install/update through the actual existing Play testing identity. Check small
   screens, keyboard access, notification permissions and text scaling.

## Remaining release gates

Backend staging/device checks, moderation ownership, deletion job monitoring,
privacy policy and public deletion-request page, Play listing/data declarations,
permanent signing credentials, and highest uploaded version code are not verified
by repository CI. This is a candidate, not a completed public release.
