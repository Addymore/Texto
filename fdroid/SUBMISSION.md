# New app: Texto (app.texto.sms)

Texto is an SMS/MMS messenger maintained by Addymore, forked from QUIK/QKSMS. The maintainer requested inclusion in F-Droid. Source: https://github.com/Addymore/Texto .

The fork has a distinct application ID, name and icon. It adds shared public/private conversation layouts, persistent number-specific PIN/biometric protection, a protected recycle bin with 30/60/90-day retention, local spam rules, reachable navigation, message/unread counters and theme controls.

## Packaging

- Application ID: `app.texto.sms`
- License: GPL-3.0-only (upstream notices retained)
- Version: 1.4.0 (2270)
- Source tag: `v1.4.0-fdroid1`
- Recipe: https://github.com/Addymore/Texto/blob/master/fdroid/metadata/app.texto.sms.yml
- Fastlane metadata: https://github.com/Addymore/Texto/tree/master/fastlane/metadata/android/en-US
- Build: OpenJDK 17, SDK/build-tools 34; `:presentation:assembleRelease`
- Release signing is disabled by default. No API key or private keystore is required.
- No Firebase/Google Services/Crashlytics plugins or runtime integrations are included.
- The MIT-licensed Call Control DataShare library supports optional integrations with proprietary blocking apps. `NonFreeAdd` is declared; Texto's local rules work independently.

## Review notes

The GitHub development APK uses `app.texto.sms.debug`, so it is not a reproducible reference binary for this release package. Please build/sign the release variant with F-Droid's key.

The privacy feature is app-level protection, not encryption of the shared Android SMS/MMS provider. RCS is not implemented. Existing device-test limitations are documented in VALIDATION.md. Screenshots contain only synthetic fixtures.

No matching Texto/app.texto.sms submission was found in the packaging-request or merge-request searches before preparation. Final inclusion and anti-feature assessment remain with F-Droid reviewers.
