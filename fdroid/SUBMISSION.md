# New app: Texto (app.texto.sms)

Submitted on 2 October 2026: https://gitlab.com/fdroid/fdroiddata/-/merge_requests/50918

Official CI awaits a maintainer-triggered run because GitLab requires identity verification for fork CI. Inclusion is pending.

Texto is an SMS/MMS messenger maintained by Addymore, forked from QUIK/QKSMS. The maintainer requested inclusion in F-Droid. Source: https://github.com/Addymore/Texto .

The fork has a distinct application ID, name and icon. It adds shared public/private conversation layouts, persistent number-specific PIN/biometric protection, a protected recycle bin with 30/60/90-day retention, local spam rules, reachable navigation, message/unread counters and theme controls.

## Packaging

- Application ID: `app.texto.sms`
- License: GPL-3.0-only (upstream notices retained)
- Version: 1.8.0 (2344)
- Source tag: `v1.8.0-fdroid1`
- Recipe: https://github.com/Addymore/Texto/blob/master/fdroid/metadata/app.texto.sms.yml
- Fastlane metadata: https://github.com/Addymore/Texto/tree/master/fastlane/metadata/android/en-US
- Build: OpenJDK 17, SDK/build-tools 34; `:presentation:assembleRelease`
- Release signing is disabled by default. No API key or private keystore is required.
- No Firebase/Google Services/Crashlytics plugins or runtime integrations are included.
- The MIT-licensed Call Control DataShare library supports optional integrations with proprietary blocking apps. `NonFreeAdd` is declared; Texto's local rules work independently.

## Validation

Texto 1.8.0 release/debug builds, 19 common JVM tests and seven Android 14 emulator suites passed. The final icon-picker layout rebuild and targeted checks also passed. The signed 1.7.3-to-1.8.0 upgrade retained synthetic conversations and the SMS role. See [VALIDATION.md](../VALIDATION.md) for exact scope, the legacy lint limitation and untested physical/carrier behavior.

The recipe pins the exact 1.8.0 application source, `074203507e6374e60ed9aa12b383891328aa7acc`. F-Droid metadata lint and source scanning passed on 20 October 2026: https://github.com/Addymore/Texto/actions/runs/39499964975/job/118894653884 . GitLab submission commit `eb5ef94a1d27a6ae8c4ebccc25774bdba25c508f` updates existing MR !50918. Pipeline https://gitlab.com/addymore/fdroiddata/-/pipelines/2969480396 created zero jobs and requests identity verification; official F-Droid inclusion remains pending.

## Review notes

GitHub now provides a production-signed `app.texto.sms` APK as well as the separate `app.texto.sms.debug` APK. Reproducibility against the production APK has not been verified. This recipe requests an unsigned build with F-Droid signing, not upstream binary verification. F-Droid and GitHub production signatures differ, so those APKs cannot update one another directly.

The privacy feature is app-level protection, not encryption of the shared Android SMS/MMS provider. RCS is not implemented. Existing device-test limitations are documented in VALIDATION.md. Screenshots contain only synthetic fixtures.

No matching Texto/app.texto.sms submission was found in the packaging-request or merge-request searches before preparation. Final inclusion and anti-feature assessment remain with F-Droid reviewers.
