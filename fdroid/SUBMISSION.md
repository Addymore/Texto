# New app: Texto (app.texto.sms)

Submitted on 2 October 2026: https://gitlab.com/fdroid/fdroiddata/-/merge_requests/50918

Official CI awaits a maintainer-triggered run because GitLab requires identity verification for fork CI. Inclusion is pending.

Texto is an SMS/MMS messenger maintained by Addymore, forked from QUIK/QKSMS. The maintainer requested inclusion in F-Droid. Source: https://github.com/Addymore/Texto .

The fork has a distinct application ID, name and icon. It adds shared public/private conversation layouts, persistent number-specific PIN/biometric protection, a protected recycle bin with 30/60/90-day retention, local spam rules, reachable navigation, message/unread counters and theme controls.

## Packaging

- Application ID: `app.texto.sms`
- License: GPL-3.0-only (upstream notices retained)
- Version: 1.6.0 (2290)
- Source tag: `v1.6.0-fdroid1`
- Recipe: https://github.com/Addymore/Texto/blob/master/fdroid/metadata/app.texto.sms.yml
- Fastlane metadata: https://github.com/Addymore/Texto/tree/master/fastlane/metadata/android/en-US
- Build: OpenJDK 17, SDK/build-tools 34; `:presentation:assembleRelease`
- Release signing is disabled by default. No API key or private keystore is required.
- No Firebase/Google Services/Crashlytics plugins or runtime integrations are included.
- The MIT-licensed Call Control DataShare library supports optional integrations with proprietary blocking apps. `NonFreeAdd` is declared; Texto's local rules work independently.

## Validation

Version 1.6.0 debug and optimized release builds passed locally; all 19 unchanged common JVM tests remain passing. Android 14 instrumentation passed three-second text selection, artwork and card controls, ordinary/protected utility access, PIN/cancel checks and 40 contrast cases. The production-signed APK passed clean-install launch checks. Full Linux Android CI is tracked separately from the metadata job. Metadata lint and source scanning passed for the 1.6.0 recipe with fdroidserver 2.4.5 on 4 October 2026: https://github.com/Addymore/Texto/actions/runs/37233479899/job/111527872393. The inherited lint configuration reports 81 non-aborting ExtraTranslation errors; this is not a clean lint result. Physical-device, carrier, real email and full backup/restore round-trip validation remain outstanding.

## Review notes

GitHub now provides a production-signed `app.texto.sms` APK as well as the separate `app.texto.sms.debug` APK. Reproducibility against the production APK has not been verified. This recipe requests an unsigned build with F-Droid signing, not upstream binary verification. F-Droid and GitHub production signatures differ, so those APKs cannot update one another directly.

The privacy feature is app-level protection, not encryption of the shared Android SMS/MMS provider. RCS is not implemented. Existing device-test limitations are documented in VALIDATION.md. Screenshots contain only synthetic fixtures.

No matching Texto/app.texto.sms submission was found in the packaging-request or merge-request searches before preparation. Final inclusion and anti-feature assessment remain with F-Droid reviewers.

Updated the existing MR to 1.6.0 (2290) on 4 October 2026; submission commit `da08f871b434b43c14a475e32daa299e7051ced8` pins the exact GitHub release source. Official inclusion remains pending.
