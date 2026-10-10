# Texto on F-Droid

Submitted on 2 October 2026: [F-Droid merge request !50918](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/50918). Texto is not yet listed in the official F-Droid repository. Inclusion requires F-Droid's independent review, build and signing process.

## Build

The proposed package is `app.texto.sms`, version 1.6.3 (2320), source tag `v1.6.3-fdroid1`. The tag pins the same application source as the GitHub 1.6.3 release (application commit 81a6e8c66227a1da33f0c99096735d27dc324d1c).

Use OpenJDK 17, Android SDK platform 34 and build tools 34.0.0:

```sh
./gradlew :presentation:assembleRelease :common:testDebugUnitTest --no-daemon
```

Output: `presentation/build/outputs/apk/release/Texto-v1.6.3-release-unsigned.apk`.
Release builds are unsigned by default. No private key, account, API key or proprietary service plugin is required. The optional `-PtextoSignRelease` enables locally configured signing for maintainers only; F-Droid must not use it.

The recipe in `metadata/app.texto.sms.yml` uses the normal release variant. The inherited custom `fdroid` build type is not used for the official submission. Dedicated `vX.Y.Z-fdroidN` tags select reviewed source revisions for F-Droid updates. Increase versionCode for each future app update.

## Validation

Version 1.6.3 debug/instrumentation builds passed locally; the 19 unchanged JVM tests remain passing (up-to-date). Final Android 14 emulator regressions passed individual-word selection/Copy, persistent archiving, ordinary/protected utility access, PIN/cancel checks, crop proportions, hue selection and 480 contrast cases. Metadata lint and source scanning passed with fdroidserver 2.4.5 on 10 October 2026: https://github.com/Addymore/Texto/actions/runs/38085383901/job/114310686687. See VALIDATION.md for release build and APK checks.

The submission recipe was updated in GitLab commit 5e544c18650402f9a71e68470b66b681560ef9c2. GitLab pipeline 2934603858 created zero jobs and requests identity verification; this is not a completed F-Droid build. Maintainer-triggered CI is still needed. The inherited non-aborting lint configuration and translation issues remain; a clean full Android lint result is not claimed. Physical-device, carrier, real email and full backup/restore round-trip validation remain outstanding.

## Listing and scope

`fastlane/metadata/android/en-US/` holds Texto's title, descriptions, icon, synthetic screenshots and version-code changelog. These replace the inherited QUIK store listing and generated repository indexes. QUIK/QKSMS code attribution and licenses remain intact.

The fork adds reachable navigation, selectable conversation/card themes, persistent number-specific privacy rules, private SMS/MMS conversations, local spam rules, unread/message counters and a protected recycle bin.

Optional integrations promote separately installed proprietary blocking apps, disclosed as `NonFreeAdd`; the local blocklist works independently. `com.callcontrol:datashare:1.3.0` is MIT-licensed on Maven Central. Unused Google Services and Crashlytics Gradle plugins have been removed. F-Droid still performs its own dependency and binary scan.

## Installation and privacy

The F-Droid package uses the same application ID as the GitHub release APK, but a different signing key, so those APKs cannot directly update one another. It is distinct from `app.texto.sms.debug`. It does not upgrade the GitHub preview or inherit its PIN, theme settings, privacy rules or bin markers. Users must configure protection before switching their default SMS app. Both apps can read the shared Android SMS/MMS provider when authorized; private and binned messages are not encrypted or hidden from other SMS apps.

RCS is not implemented. Carrier behavior and physical OnePlus frame pacing need device validation; see `VALIDATION.md` and `DEVICE-TESTS.md`.

## Submission

Recipe validation runs in GitHub Actions with fdroidserver 2.4.5. The official process is documented at https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/ . The review request is open. GitLab blocked fork CI before any jobs started because the submitting account is not identity-verified; the request asks F-Droid maintainers to trigger CI, following their contribution guidance. The updated 1.6.0 recipe passed GitHub metadata lint/source scanning; official F-Droid CI and review remain pending. Submission background is in `SUBMISSION.md`; publication must not be claimed until an official listing exists.

Updated the existing MR to 1.6.0 (2290) on 4 October 2026; submission commit `da08f871b434b43c14a475e32daa299e7051ced8` pins the exact GitHub release source. Official inclusion remains pending.
