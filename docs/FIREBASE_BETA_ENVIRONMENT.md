# Firebase beta environment

For beta03 (version code 4), debug and release use `connect-f0d00` through variant-specific google-services.json files. Package remains `com.connectapp.npl`. The root configuration for `connect-dae99` is retained for history but overridden in both current variants. Accounts and data are not migrated between projects; existing Play beta01 users may need to create a new beta account.

SERVER_FUNCTIONS_ENABLED=false in both beta variants. Push delivery and account deletion remain unavailable; this build is intended for invited internal testers. Profile explains those limits. Publishing, private profiles, joins, saves, member chat and local account-owned notifications have been tested; no public production rollout is authorized by this configuration.

The Firebase Android app is `1:358452282819:android:5dc6d971acc8f393176976`. Its default Firestore database is in asia-south1. Specify --project connect-f0d00 for any beta deployment. No Functions deployment is needed for this limited internal build.

Play requires the existing registered upload key. Never upload an AAB signed with Android CI's ephemeral validation key. Use the Play Internal Testing Bundle workflow and its connect-play-internal-aab artifact. Confirm installation, login and shared activities using the Play-delivered version before broader testing.
