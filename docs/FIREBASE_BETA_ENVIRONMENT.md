# Firebase beta environment

Debug APKs use the Connect Firebase project `connect-f0d00` through `app/src/debug/google-services.json`. Package remains `com.connectapp.npl`; release builds continue using the existing root configuration for `connect-dae99`. Existing accounts/data are not migrated between projects. Create disposable beta accounts in the debug environment.

The debug Android app is registered as `1:358452282819:android:5dc6d971acc8f393176976`. Its default Firestore database was created in `asia-south1` on 6 October 2026. Firebase client configuration contains public app identifiers, not administrative credentials.

Always specify `--project connect-f0d00` for beta deployment. Deploy indexes, the deletion worker, rules, then the callable and notification functions. Cloud Functions require Blaze billing. Enabling the Firestore API alone does not enable email/password Authentication or Cloud Functions. Confirm those separately and run the acceptance checks in BETA02_TESTING.md before distributing a new debug APK.

The existing beta02 APK built before this configuration change still points to `connect-dae99`. Download a successful Android CI artifact built from this branch or its merged commit to test `connect-f0d00`. Never use CI's ephemeral release signing key for Play uploads.
