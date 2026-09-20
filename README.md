# Connect

Connect is a native Android activity-based social app for discovering local plans, meeting people around shared interests, and creating real-world activities.

## Development status

This repository is currently being prepared for production development. The app uses Kotlin, Jetpack Compose, Material 3, Room, StateFlow, and Android Gradle Plugin 9.1.1.

Connect now has real Firebase Authentication, Firestore-backed profiles, cross-device activities, joins, saves, real-time chat, communities, Available Now, and an FCM client. Cloud Functions notification sending is implemented in the repository but remains undeployed while the Firebase project stays on the Spark plan.

## App identity

- Display name: **Connect**
- Android application ID: `com.connectapp.npl`
- Current internal-testing version: `0.2.0-beta01` (version code 2)

The application ID is intended to remain permanent for Google Play releases.

## Local setup

### Requirements
- Android Studio compatible with Android Gradle Plugin 9.1.1
- JDK 17
- Android SDK Platform 36.1
- Android SDK Build-Tools 36.x

### Open and run
1. Clone the repository.
2. Open the project in Android Studio.
3. Use JDK 17 for Gradle.
4. Install Android SDK Platform 36.1 when Android Studio prompts for it.
5. Sync the project.
6. Run the `app` configuration on an emulator or Android device.

A custom debug keystore is not required. Android's normal debug signing is used.

## Local environment

`.env` is ignored by Git and must never contain credentials that are committed to the repository.

`.env.example` contains placeholders only.

The current app does not make production Gemini API calls. Do not ship a private Gemini/server API key inside the Android APK.

## Firebase Authentication

Connect now contains a Firebase Authentication-ready account flow.

To enable real sign-in:

1. Create or open the Firebase project that will own Connect.
2. Register an Android app with package name `com.connectapp.npl`.
3. Download `google-services.json` from Firebase.
4. Place it at `app/google-services.json`.
5. In Firebase Console → Authentication → Sign-in method, enable **Email/Password**.
6. Rebuild the app.

When `google-services.json` is absent, the Google Services plugin is not applied and CI can still build the project. Debug builds show an explicit **Preview Mode** so development can continue; Preview Mode is not a real account and is not available as a production authentication substitute.

Do not add Firebase Admin SDK service-account JSON or other server credentials to this repository.

## Release signing

Release credentials are read from environment variables only when all required signing values are present:

- `KEYSTORE_PATH`
- `STORE_PASSWORD`
- `KEY_PASSWORD`
- optional `KEY_ALIAS` (defaults to `upload`)

Keystores and signing files must never be committed.

For Google Play Internal Testing, use the manual GitHub Actions workflow **Play Internal Testing Bundle** after configuring these repository secrets:

- `UPLOAD_KEYSTORE_BASE64`
- `UPLOAD_STORE_PASSWORD`
- `UPLOAD_KEY_PASSWORD`
- `UPLOAD_KEY_ALIAS`

The workflow materializes the upload keystore only inside the temporary GitHub Actions runner, builds `bundleRelease`, and uploads the signed AAB as a short-lived workflow artifact. The real upload keystore must never be committed to Git.

## CI

GitHub Actions uses:
- JDK 17
- Gradle 9.3.1
- Android SDK 36.1

CI generates a compatible Gradle wrapper, runs unit tests, and assembles a debug APK.

## Google AI Studio origin

The initial prototype was created with Google AI Studio and has since moved into normal Android/GitHub development.


## Firebase notification server

The repository includes Firebase Cloud Functions in `functions/` for trusted push sending.

Current triggers:

- a new Firestore activity chat message notifies the other joined activity members;
- a new non-host activity membership notifies the activity organizer;
- invalid or unregistered FCM device tokens are removed automatically.

The functions use the Firebase Admin SDK with the managed service identity provided by Cloud Functions. Do **not** download or commit Firebase Admin/service-account private keys for this deployment.

Runtime: Node.js 22. Functions region: `asia-south1`.

Before the first production deployment:

1. Make sure the Firebase project is on the Blaze plan. Firebase requires Blaze to deploy Cloud Functions.
2. Install the current Firebase CLI and authenticate interactively with `firebase login`.
3. From the repository root, deploy the membership collection-group index:
   `firebase deploy --only firestore:indexes`
4. Deploy the notification functions:
   `firebase deploy --only functions`

The default Firebase project alias is `connect-dae99` in `.firebaserc`.

FCM itself is handled by Firebase Cloud Messaging; the server functions use the Admin SDK in the trusted Cloud Functions runtime. Android Admin credentials must never be embedded in the app.
