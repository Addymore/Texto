# Google Play preparation

Status on 4 October 2026: not submitted. The maintainer needs to register a Play Console account. Texto 1.5.0 is available on GitHub and submitted for F-Droid review; neither is a Google Play publication.

## Confirmed build gaps

- `presentation/build.gradle` targets API 33 and compiles against API 34. New phone-app submissions currently require target API 36. Upgrade the Android toolchain and audit Android 14-16 behavior changes, permissions, background sending, notifications, scheduled messages and edge-to-edge layout before changing the release version.
- The GitHub APK uses the debug-signed `app.texto.sms.debug` package. It must not be uploaded as a production release. Build a production Android App Bundle for `app.texto.sms` and configure a privately stored upload key and Play App Signing after the account is available. Do not reuse the debug key or commit signing material.
- Audit native libraries (including Realm) for 16 KB page-size support and validate on a matching emulator/device. No compatibility result is claimed yet.
- Review default-SMS-role acquisition and loss, and the merged manifest. Submit the SMS permissions declaration for the app's core SMS/MMS functionality. A declaration is not evidence that the current implementation passes review.
- Complete Data safety, content rating, target audience, app access and store contact details from verified behavior and maintainer input. Review the existing PRIVACY.md and provide an accessible public privacy-policy URL. Do not invent a support email or mark declarations as approved.
- Test real SMS/MMS, scheduling, backup/restore and private-space behavior on current Android devices. Existing 1.5.0 tests do not establish these results.

## Account actions

Register at https://play.google.com/console/signup . Google lists a US$25 one-time registration fee, a Developer Distribution Agreement and account verification. The account owner must supply their own identity/payment information and accept the agreement.

New personal accounts are subject to a closed test with at least 12 testers continuously opted in for 14 days before applying for production access. Confirm the exact requirements displayed in the new account. A test-track upload is not public production publication.

## Existing listing assets

Use `fastlane/metadata/android/en-US/` as the starting point: title, descriptions, icon, synthetic screenshots and changelog. Retain accurate limits: SMS/MMS, no RCS; app-level private-space protection, not encryption of Android's shared message provider; SMS-only backups omit MMS attachments and privacy settings.

## Official references

- Registration: https://support.google.com/googleplay/android-developer/answer/6112435
- Target API: https://support.google.com/googleplay/android-developer/answer/11926878
- Testing: https://support.google.com/googleplay/android-developer/answer/14151465
- SMS permissions: https://support.google.com/googleplay/android-developer/answer/16558241
- Native page sizes: https://developer.android.com/guide/practices/page-sizes
