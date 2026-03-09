# The Dopest Deals v1.0

This build is patched to open and test-run in Android Studio **without requiring Firebase setup first**.

## What I fixed
- Added the missing Kotlin Android plugin.
- Added the Compose compiler plugin.
- Lowered compileSdk / targetSdk to API 35 for broader AGP compatibility.
- Removed the mandatory Google Services Gradle plugin so the app can run locally without `google-services.json`.
- Added the missing `kotlinx-coroutines-play-services` dependency required for `Tasks.await()`.

## Open in Android Studio
1. Extract the ZIP.
2. Open the project folder in Android Studio.
3. Let Gradle sync.
4. Run the `app` configuration on an emulator or device.

## Firebase leaderboard
The app still contains Firestore code, but it only runs when **Online Leaderboard** is enabled in the UI. For a plain local test run, leave that toggle off.

To enable Firebase later:
1. Create a Firebase project.
2. Register package `com.example.frontiercommoditytrader`.
3. Add `google-services.json` to `app/`.
4. If you want automatic plugin-based Firebase config processing, re-add the `com.google.gms.google-services` plugin.

## Note
This patch is aimed at getting the project to sync and run locally first. If Android Studio suggests a minor Gradle or SDK update, accepting it is fine.
