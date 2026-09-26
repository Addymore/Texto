![Texto — Messages, made personal](docs/images/banner.svg)

# Texto

A customizable Kotlin SMS/MMS app for Android by **[Addymore](https://github.com/Addymore)**, built on [QUIK](https://github.com/quik-sms/quik) and QKSMS. Reachable navigation, consistent public and private conversations, and a theme that feels like yours.

[Download 1.3](https://github.com/Addymore/Texto/releases/tag/v1.3.0) · [Changelog](CHANGELOG.md) · [Report a bug](https://github.com/Addymore/Texto/issues) · [Telegram](https://t.me/addymore)

**Development preview:** the downloadable APK is debug-signed, not a Play Store release. Android 6+; physical OnePlus 13 / Android 16 and carrier validation are still required. See [validation](VALIDATION.md).

## A look of your own

<table><tr><td><img src="docs/images/inbox.png" width="280" alt="Texto conversation cards with message totals and unread badges" /></td><td><img src="docs/images/themes.png" width="280" alt="Live theme preview and accent palettes" /></td><td><img src="docs/images/theme-options.png" width="280" alt="Card finish, spacing and preview controls" /></td></tr></table>

Screenshots use synthetic messages and sample contacts. Private screens are protected from capture.

- **One consistent layout.** Public and private lists share conversation cards, avatars, dates, message totals and unread badges. Private SMS/MMS opens in the same full conversation screen.
- **Personal themes.** Eight palettes, custom hex accents, wallpaper colors, light/dark/system appearance, AMOLED, card shapes and finishes, spacing, preview lengths, avatar/count toggles and unread dots or number badges.
- **Reachable navigation.** A collapsible large heading and floating Messages / Themes / Settings navigation. Contacts live in Settings.
- **Private conversations.** Persistent number rules, PIN/strong biometrics, silent notifications and exclusion from public search, widgets and shortcuts. Future messages from locked numbers stay private.
- **Recoverable deletion.** A protected recycle bin retains SMS/MMS attachments, with restore and 30/60/90-day expiry.
- **Control over unwanted messages.** Exact-number, prefix and phrase rules, trusted exceptions, a blocklist and recoverable quarantine.
- **The QUIK foundation.** SMS/MMS, group messages, attachments, dual SIM, scheduling, delayed sending, backups, reactions, search, pinning, speech and notification quick reply remain in the fork. Full device regression is not yet complete.

## Install

1. Download `Texto-1.3.0-debug.apk` from the [release](https://github.com/Addymore/Texto/releases/tag/v1.3.0).
2. Install it and select Texto as your default SMS app. Allow the permissions needed for messaging and contacts.
3. Personalize it in **Themes** and configure your privacy PIN and number rules in **Settings → Privacy & protection**.

The development app ID is `app.texto.sms.debug`, so it can coexist with QUIK. An upgrade from an earlier Texto development build must use the same signing certificate. Keep a verified backup before replacing your primary SMS workflow.

## Privacy and limitations

Texto privacy is **app-level protection**, not encryption of Android's shared SMS/MMS provider. Other SMS-authorized apps, root access and exported backups are outside this protection. The bin keeps provider records until expiry; clearing Texto data removes its local deletion markers. Background cleanup may be deferred by Android.

RCS is not implemented: ordinary third-party SMS apps cannot provide the privileged carrier/OEM IMS integration it needs. SMS delivery reports depend on the carrier; SMS read receipts are not claimed. High-refresh display requests depend on your device and power settings.

See [privacy details](PRIVACY.md), [device checks](DEVICE-TESTS.md) and [validation results](VALIDATION.md).

## Build

Use **JDK 17**, Android SDK **34**, and the included Gradle wrapper. Set `sdk.dir` in a local `local.properties` file.

```sh
./gradlew :presentation:assembleDebug :common:testDebugUnitTest
```

On Windows:

```powershell
./build-texto.ps1
```

The Windows helper uses a short Unix-socket path for JDK 17. APK output is in `presentation/build/outputs/apk/debug/`. A private release keystore is required for production distribution; no signing keys are included. CI artifacts are independently debug-signed and are not guaranteed to upgrade a downloaded release APK.

## Contribute and support

See [CONTRIBUTING.md](CONTRIBUTING.md). Report the Texto version, phone/Android version and reproduction steps. Remove real phone numbers and message contents from screenshots and logs.

Texto developer: **Addymore** · [Telegram @addymore](https://t.me/addymore).

## Credits and license

Texto derives from QUIK commit `4b942fe71ceaa56f43266de115d14aa5b30d99db`. Thank you to QUIK contributors, Moez Bhatti and QKSMS, and Jake and Luke Klinker for android-smsmms. Original notices and history are retained; [UPSTREAM.md](UPSTREAM.md) preserves the upstream README.

[GNU GPL v3 or later](LICENSE), consistent with inherited source notices. Corresponding source accompanies the APK. The design draws inspiration from Samsung Messages, iOS navigation and ColorOS; Texto is independent and is not affiliated with those vendors.
