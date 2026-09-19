# Connect

Connect is a native Android activity-based social app for discovering local plans, meeting people around shared interests, and creating real-world activities.

## Development status

This repository is currently being prepared for production development. The app uses Kotlin, Jetpack Compose, Material 3, Room, StateFlow, and Android Gradle Plugin 9.1.1.

Current product data is primarily local/on-device. Real authentication, backend sync, real-time chat, push notifications, and production identity verification are planned work.

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

## Release signing

Release credentials are read from environment variables only when all required signing values are present:

- `KEYSTORE_PATH`
- `STORE_PASSWORD`
- `KEY_PASSWORD`
- optional `KEY_ALIAS` (defaults to `upload`)

Keystores and signing files must never be committed.

## CI

GitHub Actions uses:
- JDK 17
- Gradle 9.3.1
- Android SDK 36.1

CI generates a compatible Gradle wrapper, runs unit tests, and assembles a debug APK.

## Google AI Studio origin

The initial prototype was created with Google AI Studio and has since moved into normal Android/GitHub development.
