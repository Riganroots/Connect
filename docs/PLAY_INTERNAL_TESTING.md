# Google Play Internal Testing

Connect package: `com.connectapp.npl`

Current beta: `0.2.0-beta01`  
Version code: `2`

## 1. Create the Play upload key once

Keep this file private and backed up. Do not commit it to Git and do not share the private keystore.

Example with JDK 17+:

```bash
keytool -genkeypair -v \
  -keystore connect-upload-key.jks \
  -alias upload \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Use strong passwords and store them in a password manager.

Google Play App Signing should manage the app-signing key. The local `connect-upload-key.jks` is the separate upload key used to authenticate future AAB uploads.

## 2. Add GitHub Actions secrets

Repository → Settings → Secrets and variables → Actions → New repository secret.

Required secrets:

- `UPLOAD_KEYSTORE_BASE64`
- `UPLOAD_STORE_PASSWORD`
- `UPLOAD_KEY_PASSWORD`
- `UPLOAD_KEY_ALIAS` (normally `upload`)

### Convert the keystore to Base64 on Windows PowerShell

Run from the folder containing the keystore:

```powershell
[Convert]::ToBase64String(
  [IO.File]::ReadAllBytes((Resolve-Path ".\connect-upload-key.jks"))
) | Set-Clipboard
```

Paste the clipboard value into `UPLOAD_KEYSTORE_BASE64`.

Do not paste the keystore or its passwords into issues, commits, pull requests, chat logs, or source files.

## 3. Build the signed AAB

GitHub repository → Actions → **Play Internal Testing Bundle** → Run workflow.

If all four signing secrets are configured, the workflow:

1. runs unit tests;
2. builds the signed release Android App Bundle;
3. uploads an artifact named `connect-play-internal-aab`.

Download the AAB artifact after the workflow succeeds.

## 4. First Play Console upload

In Google Play Console:

1. Create/select **Connect**.
2. Confirm the package/application ID is `com.connectapp.npl`.
3. Configure **Play App Signing** when prompted.
4. Go to Test and release → Internal testing.
5. Create a release.
6. Upload the signed `.aab`.
7. Add concise beta release notes.
8. Add tester emails or a Google Group.
9. Review and roll out the Internal Testing release.
10. Share the tester opt-in link only with intended testers.

Google Play supports up to 100 Internal Testing testers.

## 5. Suggested beta release notes

```text
Connect beta

• Discover and create local activities
• Join, save and chat across devices
• Communities and Available Now
• Firebase account and cloud sync
• Mobile UI improvements

This is an early testing build. Please report login, sync, activity, chat or layout issues.
```

## 6. Before broader testing

Before moving beyond Internal Testing:

- finish the Play Store listing;
- provide a public privacy-policy URL;
- complete Data safety accurately;
- complete content rating;
- configure app access if any reviewer credentials are required;
- review countries/regions and pricing;
- verify account-deletion requirements;
- test installation from Google Play rather than only sideloaded APKs;
- keep increasing `versionCode` for every uploaded Play build.

If the developer account is subject to Google Play's newer personal-account production-access rules, complete the required closed test before applying for production access.
