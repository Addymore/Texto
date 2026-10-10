# New app: Texto (app.texto.sms)

Submitted on 2 October 2026: https://gitlab.com/fdroid/fdroiddata/-/merge_requests/50918

Official CI awaits a maintainer-triggered run because GitLab requires identity verification for fork CI. Inclusion is pending.

Texto is an SMS/MMS messenger maintained by Addymore, forked from QUIK/QKSMS. The maintainer requested inclusion in F-Droid. Source: https://github.com/Addymore/Texto .

The fork has a distinct application ID, name and icon. It adds shared public/private conversation layouts, persistent number-specific PIN/biometric protection, a protected recycle bin with 30/60/90-day retention, local spam rules, reachable navigation, message/unread counters and theme controls.

## Packaging

- Application ID: `app.texto.sms`
- License: GPL-3.0-only (upstream notices retained)
- Version: 1.6.3 (2320)
- Source tag: `v1.6.3-fdroid1`
- Recipe: https://github.com/Addymore/Texto/blob/master/fdroid/metadata/app.texto.sms.yml
- Fastlane metadata: https://github.com/Addymore/Texto/tree/master/fastlane/metadata/android/en-US
- Build: OpenJDK 17, SDK/build-tools 34; `:presentation:assembleRelease`
- Release signing is disabled by default. No API key or private keystore is required.
- No Firebase/Google Services/Crashlytics plugins or runtime integrations are included.
- The MIT-licensed Call Control DataShare library supports optional integrations with proprietary blocking apps. `NonFreeAdd` is declared; Texto's local rules work independently.

## Validation

Version 1.6.3 debug/instrumentation builds passed locally; the 19 unchanged JVM tests remain passing (up-to-date). Final Android 14 emulator regressions passed individual-word selection/Copy, persistent archiving, ordinary/protected utility access, PIN/cancel checks, crop proportions, hue selection and 480 contrast cases. Metadata lint and source scanning passed with fdroidserver 2.4.5 on 10 October 2026: https://github.com/Addymore/Texto/actions/runs/38085383901/job/114310686687. See VALIDATION.md for release build and APK checks.

The submission recipe was updated in GitLab commit 5e544c18650402f9a71e68470b66b681560ef9c2. GitLab pipeline 2934603858 created zero jobs and requests identity verification; this is not a completed F-Droid build. Maintainer-triggered CI is still needed. The inherited non-aborting lint configuration and translation issues remain; a clean full Android lint result is not claimed. Physical-device, carrier, real email and full backup/restore round-trip validation remain outstanding.

## Review notes

GitHub now provides a production-signed `app.texto.sms` APK as well as the separate `app.texto.sms.debug` APK. Reproducibility against the production APK has not been verified. This recipe requests an unsigned build with F-Droid signing, not upstream binary verification. F-Droid and GitHub production signatures differ, so those APKs cannot update one another directly.

The privacy feature is app-level protection, not encryption of the shared Android SMS/MMS provider. RCS is not implemented. Existing device-test limitations are documented in VALIDATION.md. Screenshots contain only synthetic fixtures.

No matching Texto/app.texto.sms submission was found in the packaging-request or merge-request searches before preparation. Final inclusion and anti-feature assessment remain with F-Droid reviewers.

Updated the existing MR to 1.6.3 (2320) on 10 October 2026; submission commit `5e544c18650402f9a71e68470b66b681560ef9c2` pins application source `81a6e8c66227a1da33f0c99096735d27dc324d1c`. Official inclusion remains pending.
