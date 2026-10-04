# Firestore access policy

This repository now includes proposed access rules matching the current Android
collections. They have not been deployed to the live Firebase project.

## Policy

- Signed-in users can discover activities and availability.
- Profiles (which may contain email), push tokens, saved activities, group
  selections, and joined-activity documents are readable only by their owner.
- Activity creation requires the organizer's membership in the same batch.
- Joining/leaving requires the caller's membership and the activity count to
  change atomically. Clients cannot edit another user's membership, activity
  content, organizer identity, or verification status.
- Activity members and organizers can read chat and send messages under their
  own UID. Outsiders cannot read or send; members cannot edit old messages.
- Unknown paths are denied. Client deletion of profiles/activities is deliberately
  denied until account cleanup is implemented. Admin SDK server operations bypass
  these rules and require their own authentication/authorization.
- Group membership records currently represent personal selections of static
  groups; they do not grant access to any group chat.

Display names are user supplied, not verified identity. Ratings and verification
on newly created activities are zero/false; only a trusted backend may award them.
Participant capacity is not enforced here: the current app does not define whether
participantsNeeded includes the host. Agree that behavior before adding a limit.

## Automated verification

Use Node.js 22 and Java 21:

```sh
cd rules-tests
npm ci --no-audit --no-fund
npm test
```

Tests use the isolated `demo-connect-rules` project and Firestore emulator, not
live data. CI runs the same positive and adversarial cases, including the query
shapes used by Android. Both Android CI and Firestore Rules CI should pass before
merging this change. Dependencies are locked in rules-tests/package-lock.json for reproducible CI.

## Before live deployment

Export and compare the current live rules against this file; their current state
has not been inspected. Verify existing profile/activity fields and membership
counts with two real test accounts. Older documents may not meet strict schemas,
so repair incompatible test data before replacing live rules. Activity discovery
is available to every authenticated account; this app currently has no private
activity model. Availability reads include expired documents, which Android
filters locally.

After emulator tests and staging/device checks pass, deploy only rules with the
explicit intended Firebase project. Do not use an unrestricted Firebase deploy
command, which could deploy functions too. A rules change affects installed older
versions immediately and needs a saved copy of the previous rules for rollback.

Official references:
- https://firebase.google.com/docs/rules/unit-tests
- https://firebase.google.com/docs/reference/rules/rules.firestore
