# Account deletion

Profile → Delete account asks for the current password and an irreversible-action
confirmation. Android reauthenticates, refreshes the ID token, and invokes
`requestAccountDeletion` in asia-south1. The callable accepts no target UID or path;
it derives the user exclusively from verified authentication and requires an
authentication time within five minutes.

The callable creates an admin-only `accountDeletionJobs/{uid}` record and returns
acceptance. The app stops its cloud listeners, signs out, clears the Room mirror,
and says deletion is requested, not completed. Failed or unconfirmed requests
show an error. A timed-out request may already have been accepted; retrying is
safe because only one job exists per UID. Passwords are never sent to the callable.

Rules freeze the account once the job exists, including already issued tokens,
and prevent new joins, saves or chat writes into its hosted activities. Notification
sending also suppresses messages to/from deleting accounts. The cleanup worker
uses retries and admin access to remove:

- hosted activities and their nested chats, plus other users' saved/joined links;
- the user's memberships in other activities, decrementing counts transactionally;
- authored messages in other activities and block references targeting the UID;
- reports submitted by or about the UID, availability, profile and all user
  subcollections (including device tokens, saves, selections and owned blocks);
- Firebase Authentication, last, after successful data cleanup.

Hosted activity paths are durably recorded before recursive deletion, so a retry
can remove descendants even if an earlier attempt already removed the parent.
The process is not globally atomic; partial cleanup can be visible while retrying.
The minimal UID-keyed job/status record remains as a tombstone to deny stale-token
access. No user profile, password or message content is retained in that record.
Deleting hosted chats also deletes other participants' messages in those chats.

## Rollout and limits

No live account has been deleted or Firebase deployment performed by this change.
Deploy the required collection-group indexes first, then the worker, rules and
callable before exposing the new APK. Existing block records without `targetUid`
need backfilling before rollout; new writes include that cleanup query field.
Compare live rules/data first, including legacy message/membership field names.
The worker only covers the repository's current data model; other storage buckets,
external analytics, backups or records outside these collections are not covered.
Firestore SDK disk cache and copies on other devices are not erased by clearing
this device's Room mirror. Existing devices lose server access when the job starts.

Blaze and deployed Cloud Functions are required. An undeployed callable fails
instead of pretending deletion succeeded. Monitor queued/processing jobs and
function errors; fix permanent failures and rerun cleanup through trusted admin
access if retries expire. Deploy the worker before the callable so newly created
jobs always have a trigger. No completion email, public deletion web page or
service-time guarantee is included. Do not claim release readiness until these
remaining operational and device checks are complete.

## Verification

- 22 emulator checks passed, including access/report/block rules and real Admin
  Firestore cleanup of 127 authored messages, foreign references and nested chats.
- The cleanup test injects a partial recursive-delete failure and verifies retry
  safety, preserved unrelated data and no double-decrement of membership counts.
- 9 server unit checks passed, including own-UID authorization, stale authentication,
  forged targets, final Auth deletion prevention after cleanup failure and push guards.
- Authentication deletion itself is mocked in local integration testing; verify
  real Auth account disabling/deletion and the callable/trigger in a staging project.
- Android CI and device checks must verify password failure, cancel, offline timeout,
  accepted-request sign-out, local cleanup and subsequent login rejection.
