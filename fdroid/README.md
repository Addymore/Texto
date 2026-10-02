# Texto on F-Droid

Publication is being prepared. Texto is not yet listed in the official F-Droid repository. Inclusion requires F-Droid's independent review, build and signing process.

## Build

The proposed package is `app.texto.sms`, version 1.4.0 (2270), source tag `v1.4.0-fdroid1`. The tag contains packaging changes after the GitHub development preview, without changing the application version.

Use OpenJDK 17, Android SDK platform 34 and build tools 34.0.0:

```sh
./gradlew :presentation:assembleRelease :common:testDebugUnitTest --no-daemon
```

Output: `presentation/build/outputs/apk/release/Texto-v1.4.0-release-unsigned.apk`.
Release builds are unsigned by default. No private key, account, API key or proprietary service plugin is required. The optional `-PtextoSignRelease` enables locally configured signing for maintainers only; F-Droid must not use it.

The recipe in `metadata/app.texto.sms.yml` uses the normal release variant. The inherited custom `fdroid` build type is not used for the official submission. Dedicated `vX.Y.Z-fdroidN` tags select reviewed source revisions for F-Droid updates. Increase versionCode for each future app update.

## Validation

The local unsigned release build passed in 6m 26s and all 19 JVM tests passed. The inherited lint configuration reports 81 ExtraTranslation errors for unused translated strings and does not stop the build; these are not a clean lint result. A local-only signed copy of the optimized release installed and opened its synthetic inbox on Android 14. F-Droid metadata lint and source scanning passed in Linux CI on 2 October 2026 with fdroidserver 2.4.5 against tag `v1.4.0-fdroid1` ([validation job](https://github.com/Addymore/Texto/actions/runs/36987182850/job/110774771350)). Signing the local test copy does not change the unsigned build artifact.

## Listing and scope

`fastlane/metadata/android/en-US/` holds Texto's title, descriptions, icon, synthetic screenshots and version-code changelog. These replace the inherited QUIK store listing and generated repository indexes. QUIK/QKSMS code attribution and licenses remain intact.

The fork adds reachable navigation, selectable conversation/card themes, persistent number-specific privacy rules, private SMS/MMS conversations, local spam rules, unread/message counters and a protected recycle bin.

Optional integrations promote separately installed proprietary blocking apps, disclosed as `NonFreeAdd`; the local blocklist works independently. `com.callcontrol:datashare:1.3.0` is MIT-licensed on Maven Central. Unused Google Services and Crashlytics Gradle plugins have been removed. F-Droid still performs its own dependency and binary scan.

## Installation and privacy

The F-Droid package is distinct from `app.texto.sms.debug`. It does not upgrade the GitHub preview or inherit its PIN, theme settings, privacy rules or bin markers. Users must configure protection before switching their default SMS app. Both apps can read the shared Android SMS/MMS provider when authorized; private and binned messages are not encrypted or hidden from other SMS apps.

RCS is not implemented. Carrier behavior and physical OnePlus frame pacing need device validation; see `VALIDATION.md` and `DEVICE-TESTS.md`.

## Submission

Recipe validation runs in GitHub Actions with fdroidserver 2.4.5. The official process is documented at https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/ . GitLab account verification is needed to submit the recipe or packaging request. A prepared request is in `SUBMISSION.md`; publication must not be claimed until an official listing exists.
