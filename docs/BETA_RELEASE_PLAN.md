# Connect beta release plan

Connect is developed only in `Riganroots/Connect`. Keep application ID
`com.connectapp.npl`. Connect-Nepal and Connect-V2 are reference implementations,
not parallel products. Do not migrate Firebase projects or signing identities
without first checking the existing testing release.

## Development workflow

- Both Claude and Codex use this repository and small task branches.
- Assign one implementer per task. The other tool can review the pull request.
- Run unit tests, debug assembly, and release bundle checks before merging.
- Distinguish code implementation, passing CI, and verified device behavior.

## Remaining release work

Source review on 2026-10-04 found these items; live Firebase configuration and
the existing Play testing release have not been inspected in this session.

1. Fix Android Back behavior on Create Plan, Profile, and Chat; close the profile
   editor before navigating away. Reset navigation when the active user changes.
2. Add version-controlled Firestore security rules and emulator tests. Currently
   firebase.json references indexes but no rules file. Verify live rules before
   deploying replacements; their absence here does not establish live exposure.
3. Implement and verify account deletion, reporting, and blocking. No matching
   implementations were found in the current Android source review.
4. Verify two real accounts: create/join/leave/save an activity, exchange chat
   messages, reconnect after network loss, and sign out/switch users without
   seeing the previous user's private content.
5. Verify notification sending on the deployed backend. README describes server
   functions as undeployed; check actual deployment and billing state first.
6. Confirm privacy information, signing identity, version code, and install/update
   behavior against the existing testing release before a new beta upload.

## Navigation device checks

- Home: system Back retains normal Android exit behavior.
- Profile, Create Plan, Chat: system Back returns to Home.
- Profile editor: Back closes the editor first.
- Switching accounts: the newly authenticated session starts at Home.
- Create Plan: Back preserves the draft; navigating back to Create Plan allows
  editing it. Publishing continues to return to Home.

These checks must be performed on a device or emulator; build success alone is
not evidence that they passed.
