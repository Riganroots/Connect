# Connect flow audit — 9 October 2026

Scope: Riganroots/Connect, main 8d036c4, plus this audit branch. Kotlin/Compose screens, Room mirror, Firebase repositories, Firestore rules, notification service and build configuration were traced. No phone or authenticated production-console testing was performed. User reports: publishing appears to do nothing and activity is missing when switching accounts. Those live symptoms remain unconfirmed until the corrected APK is tested against its backend.

## Flow findings

| Flow | Finding and evidence | Status / priority |
| --- | --- | --- |
| Publish | CreatePlanScreen lacked saving state and did not render the ViewModel's write error. Multiple taps could submit multiple new document IDs. | Fixed: progress, disabled repeat submission, separate visible publish error. P0 |
| Publish validation | Zero people and text beyond rule limits were submitted and rejected by rules. ViewModel silently defaulted invalid people to four. | Fixed: required fields, numeric range and backend text limits. P0 |
| Publish navigation | Success navigated to Home, whose locally remembered tab defaulted to Explore. New activities appear only under Community. Existing filters could conceal the new post. | Fixed: Community selected and filters reset after confirmed publish. P0 |
| Publish trust fields | Client copied a local profile rating even though real activity rules require rating zero. Legacy cached ratings could cause rejection. | Fixed: real-account publishing always sends zero. P0 |
| Room feed mirror | Publish and snapshot sync could independently insert a new local row for the same cloud ID. Whole-feed replacement used several non-transactional writes. | Fixed: transactional upsert and snapshot synchronization. P1 |
| Profile activity lists | Profile read the same filtered plans as Home. Searching or selecting a category/area changed Hosted, Joined and Saved counts. | Fixed: Profile uses the session's unfiltered activity list. P1 |
| Account switching | Search/category/area persisted when changing account. | Fixed: filters and Home tab reset on account change. P1 |
| Profile editing | updateProfileInfo only writes Room. CloudUserProfileRepository is unused and offers initialization, not edit synchronization. Reinstall/another device loses edited name, bio, interests and traveller preference. | Open: implement cloud profile read/write with schema and error handling. P1 |
| Local notifications | AppNotification has no owner UID; allNotifications reads the whole table. Account B sees A's local reminders and demo welcome rows. | Open: owner-scoped schema, migration and queries. P1 |
| Authentication | Sign-in/signup await Firebase tasks without bounded UI recovery; cloud-profile initialization is callback-based and has no timeout. Offline requests can leave loading indefinitely. | Open: pending/offline status and safe recovery; cancellation must not misrepresent server acceptance. P1 |
| Feed recovery | Any activity/joined/saved listener error closes the combined feed. ViewModel catch ends observation. No retry control restarts it; sign-in currently restarts listeners. Private listeners also initially emit an empty activity list before the activity listener loads. | Open: explicit loading, sync state and retry; avoid emitting incomplete initial snapshots. P1 |
| Feed visibility across accounts | Activities are globally readable by signed-in users; joined and saved selections are private to each account. | Emulator verified, including exact second-account query results. Live device/project still needs verification. |
| Join/leave | Transaction and rules atomically update membership/count; host leave is disallowed. Repeated taps can queue toggles, with no per-card pending state. | Backend verified; pending action feedback remains open. P2 |
| Saved | Private account-owned Firestore selections; errors shown on Home, not Profile. | Backend verified; show errors/pending state on Profile. P2 |
| Chat | Joined-member-only activity chat works at rules level. Composer clears text immediately before server success and truncates messages above 1,000 characters. No pending/retry state; a rejected message loses its draft. | Open: preserve draft until acknowledgement and show length/pending state. P1 |
| Groups | Group rows and displayed member counts are static seeded content. Joining saves only personal membership, not a shared group roster; no group discussion backend exists. | Product limitation: label correctly or implement actual community features. P2 |
| Available Now | Shared status, owner-only writes and two-hour expiry are implemented. Dialog closes before write success; groups and availability share one error variable, and another successful listener can clear that error. | Rules verified; isolate action errors and pending state. P2 |
| Blocking | Own blocklist hides host activities and sender messages; client filtering does not deny access to public activity documents. Server push guards check both directions. | Backend tests pass; phone UI test pending. |
| Reporting | Joined participants can report a host. Clients cannot read reports; moderation requires manual trusted-console review. | Rules verified; actual report submission and moderation test pending. |
| Notifications | Server Functions have not been deployed. Notification intents include type/target extras, but MainActivity does not consume those extras to open the relevant activity/chat. | Expected unavailable in current debug beta; deep links remain open. P1 before push release |
| Account deletion | Debug disables the button and gateway. Release enables it although its Functions backend is not deployed and targets a different Firebase project. | Expected unavailable in testing; release blocker. |
| Build/backend identity | Debug targets connect-f0d00; release targets connect-dae99. Both APKs use the same app ID and version name/code. A screenshot alone cannot establish which variant/project is installed. | Open: visible beta build identity; reconcile release project before distribution. P0 before release |
| Navigation | System back returns secondary screens to Home; custom sealed Screen navigation has no activity-detail/back-stack persistence. | Code checked; recreation/device interaction pending. |
| Activity lifecycle | No host edit, cancellation, deletion or structured expiry flow. Dates are free text; old activities remain in feed and "more wanted" is not decremented by joins. | Open product scope for beta; clarify capacity semantics. P2 |
| Test coverage | Existing Android tests mainly cover sample resources/rendering, not end-to-end two-account workflows. | Added form-validation and Room mirror regression tests. Phone tests remain required. |

## Verification

- Baseline: 22 Firestore rules/cleanup emulator tests passed; 9 server unit tests passed.
- Added second-account regression checks exact published activity identity/title, empty private selections, joining and resulting host count. Updated emulator suite: 23 tests passed.
- Added Android validation tests and a Room test for concurrent upsert, stable IDs, selection refresh and preservation of preview rows.
- Local Android execution blocked before compilation by unavailable Gradle distribution download. GitHub Android CI is the validation route; consult this PR's checks for the final result.
- No live publishing, multi-account phone session, Firebase Auth email delivery, release deployment or push delivery was tested here.

## Device acceptance sequence

Use the debug APK produced by this PR's successful Android CI. Use two disposable real accounts; do not use Preview Mode.

1. Account A signs in, edits its profile, creates a uniquely titled activity with all required fields. Publish shows saving state; once acknowledged it opens Community and the activity is visible.
2. Restart A: the activity remains. Sign out and sign in as B: Community shows the same activity, Hosted is empty for B, Joined/Saved reflect only B.
3. B saves, joins, opens chat and sends one message. A receives the message in that activity's chat and sees the updated count. B leaves and loses chat access.
4. Test a missing field, zero people, oversized title, and offline publish. Validation is visible; pending writes are not called successful before server acknowledgement. Reconnect before attempting another publish.
5. Apply Home filters, then open Profile: personal counts remain independent of filters. Switch accounts: Home filters reset.
6. Block/unblock host; verify activity/chat filtering. Report as a joined participant and verify privately in trusted moderation access.
7. Check the known open gaps above separately. Push and account deletion are not acceptance requirements for this limited debug test, but they block a public product release.

## Recommended order

Complete publishing/live-project verification first, then cloud profile sync and notification account isolation, then chat acknowledgement and listener recovery. Reconcile release Firebase configuration and deploy required Functions before public distribution. Do not start a new repository while these beta blockers remain.
