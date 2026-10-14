# Android 15–17 compatibility review — 14 October 2026

Texto 1.7.1 adds compatibility-oriented improvements without changing the existing install/upgrade requirements. Android 15 and newer remain within the supported OS range; the minimum remains Android 6. This is not a claim of completed Android 17 device certification.

## Improvements available on current and newer Android versions

- Notification, widget and sharing-shortcut avatars now use bounded image requests, a maximum 512-pixel dimension and a three-second timeout. Every completed or failed request is released. System UI receives an independent bitmap copy, so releasing Glide's pooled resource cannot invalidate a displayed icon. Sharing shortcuts previously requested the original photo size.
- SMS/MMS body selection uses the platform's native handles and Copy actions inside the existing message bubble. No private Android editor or MessageQueue fields are accessed.
- Camera capture explicitly grants read/write URI access; attachment sharing and backup exports explicitly grant read access. These changes follow the platform's recommendation to stop relying on implicit grants.
- Search retains its 500-result cap and cancels stale result delivery. Unchanged conversation styling reuses its existing background without requesting layout.
- Existing default-SMS role and protected-content rules remain in place. No OTP protection bypass or new permission is introduced.
- OEM-inspired styles are optional visual profiles. They do not require a manufacturer framework, alter the user's current theme until Apply is selected, or claim to embed Samsung/OPPO applications.

## Current validation and remaining migration

The build still uses compile SDK 34 and target SDK 33. API 37-only APIs and target-37 behavior, including the new target-dependent MessageQueue implementation, are not enabled by this release. A full target-SDK migration needs a coordinated build-tool, native-library, background-work, permission and edge-to-edge review. The bundled arm64 Realm library has 4 KB ELF LOAD alignment (verified from the APK), so native 16 KB page-size support is not established. Rebuilding/upgrading that dependency and testing retained databases is required; changing the APK alignment or SDK number alone is insufficient.

Automated regression and signed upgrade checks use the Android 14 emulator. A Samsung A57 compatibility check has been requested, but no physical device was visible over USB at the time of this review. Carrier SMS/MMS, OTP delivery, fingerprint hardware and physical Android 15/16/17 performance remain unverified. See [validation notes](../VALIDATION.md) for the actual test evidence.

## Platform references

- [Android 17 changes affecting all apps](https://developer.android.com/about/versions/17/behavior-changes-all): memory limits, OTP behavior and explicit URI grants.
- [Android 17 target-dependent changes](https://developer.android.com/about/versions/17/behavior-changes-17): RemoteViews memory and MessageQueue changes.
- [Android 17 SDK setup](https://developer.android.com/about/versions/17/setup-sdk): SDK/toolchain prerequisites.
