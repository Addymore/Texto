# Texto validation — 4 October 2026

## 1.6 automated and emulator checks

- Debug, optimized unsigned release and instrumentation builds passed in 10m 3s. All 19 unchanged common JVM tests remain passing (Gradle up-to-date results). The inherited non-aborting lint configuration is unchanged.
- Both downloadable APKs passed Android signature verification and 4-byte alignment. The debug certificate matches 1.5.0 and the debug APK upgraded the existing synthetic emulator installation without clearing app data.
- The production APK is signed with the new Texto release certificate, package `app.texto.sms`, version 1.6.0 (2290). A clean installation launched successfully on the Android 14 emulator, including after granting the SMS role; no crash was recorded. The separate debug package is `app.texto.sms.debug`, version 1.6.0-debug (2290).
- Gesture instrumentation passed: a 700 ms hold selects the message; text remains unselectable at 1.5 seconds and native word selection opens after the three-second threshold. Scrolling and binding another message cancel/reset text selection. This test exercises the shared gesture helper with a real TextView; physical touch and OEM selection menus remain device checks.
- Privacy scope tests passed for ordinary, locked, archived and additional protected numbers, including mixed-recipient groups. Real activity tests confirmed ordinary Backup, Message management and Scheduled open without authentication, while protected variants stay invisible and secure until PIN verification. Cancellation closes the protected utility.
- Custom title/motto and loaded background/header artwork assertions passed. Card-size scaling and transparent No cards assertions passed. The drawer and customized inbox screenshots were visually inspected using synthetic conversations.
- Contrast instrumentation passed all 40 incoming/outgoing, light/dark/AMOLED and custom-accent cases, including phone/web links and recycled views, plus Settings card tint and scheduled-picker checks.
- Initial test attempts were blocked by the emulator's device keyguard. After removing that synthetic device lock and rebooting, the full regression passed. Texto's separate app PIN remained enabled for the protection tests.
- Physical OnePlus performance, real carrier SMS/MMS, actual fingerprint sensing, real email delivery and a complete backup/restore round trip were not tested. Backups remain SMS-text-only and do not contain MMS attachments or app-private settings.
- F-Droid MR 50918 currently proposes the 1.5.0 source recipe and awaits upstream review; this 1.6.0 GitHub release does not indicate F-Droid or Google Play publication.

Release certificate SHA-256: `ca8e5a7493fd4b607335d51f1643666e81bd8a4df7a6cd34dc924175b21100a4`.

See [APK signing and upgrade notes](docs/RELEASE-SIGNING.md) before switching between release, debug and F-Droid packages.

## 1.5 automated and emulator checks

- Debug and unsigned release builds passed in 7m 36s; all 19 JVM tests passed. Existing 81 ExtraTranslation lint errors remain under the inherited non-aborting configuration.
- Debug APK signature and zip alignment passed. The APK installed over the previous development build on the synthetic Android 14 emulator.
- A dedicated test exercised the real utility/PIN activity round trip for Backup, Message management and Scheduled. Each screen stayed hidden and secure before authentication, resumed after a valid PIN, and cancellation closed the screen without exposing messages. The test uses synthetic PIN/number rules and restores the prior test preferences.
- The existing contrast instrumentation passed all 40 incoming/outgoing, light/dark/AMOLED and custom-accent cases, plus Settings tint and scheduled-picker checks.
- Visual checks verified Messages/Themes-only bottom navigation, Settings reached through the three-dot menu, tonal Settings cards and icon badges using the selected Jade accent, and the full Themes editor opened from Settings.
- The email feature shares an explicitly selected saved JSON backup through Android's chooser. No real email was sent. Real email-provider delivery and a full backup/restore round trip remain unverified; this format supports SMS text, not MMS attachments or Texto privacy settings.
- Physical OnePlus/carrier behavior and performance measurements remain unverified. The F-Droid submission still targets the previously validated 1.4.0 source revision.


## 1.4 automated and emulator checks

- Final debug build passed (final resource rebuild: 1m 54s). All 19 JVM tests passed (zero failures/errors).
- Final APK signature verification and 4-byte alignment passed. Application ID `app.texto.sms.debug`, version `1.4.0-debug`, version code 2270. Installation over the previous development build succeeded without clearing data.
- Android 14 instrumentation passed 40 contrast combinations using the real incoming/outgoing text views, including light/dark/AMOLED, black/white/blue/Jade/gray accents, recycled views, phone/web URL spans and confirmation spans. Text/link contrast was at least 4.5:1 and links remained underlined.
- The same instrumentation verified Jade and Rose primary/platform accents, the scheduling date-picker accent and Settings cards without a fixed blue background tint.
- Final Settings visual inspection confirmed neutral conversation-style cards, readable shortcut labels and the selected Jade accent, with the XML tint override removed.
- An actual synthetic phone-number/web-link message was visually checked in light and dark conversation screens. Eight alternating flings through 27 synthetic SMS completed with no crash log entries.
- The About screen displayed Texto repository/changelog URLs and Addymore attribution. Source inspection verified About/share links now target Addymore/Texto. The legacy global color picker disables wallpaper and automatic colors when applying a chosen color.
- Scrolling optimizations remove attachment adapters from plain SMS rows, reuse MMS adapters/click streams, avoid unchanged avatar reloads, simplify line layout and remove redundant conversation-theme work. These are implementation improvements, not a measured OnePlus frame-rate claim.
- Exact custom control/dialog colors use the pinned Material 1.11 resource loader on Android 11+. Android 6–10 retains the fallback control palette and needs separate visual validation.
- Physical OnePlus frame pacing, real carrier traffic and a full mixed-media regression remain unverified. Prior private-space tests below describe 1.3 and were not all repeated for 1.4.


## 1.3 automated checks

- Final Android development APK built successfully in 3m 26s.
- 19 JVM tests passed with zero failures/errors: RuleEngine 6, VaultSession 5, TrashRetention 4, TwoFingerPull 4.
- Android apksigner verified v1/v2 signatures and one debug signer; zipalign 4-byte alignment passed. Standard META-INF v1-signature warnings remain; whole-APK v2 verification passes.
- Application ID `app.texto.sms.debug`; version `1.3.0-debug` (2260); minimum API 23, target API 33, compile API 34. Original dependency/deprecation warnings remain.

## 1.3 Android 14 emulator checks

A dedicated AOSP x86_64 emulator used synthetic messages and contacts. No carrier messages were sent.

- Separate test APK returned PASS for the shared conversation adapter, private-only rows, public inbox exclusion, secure private conversation window, shared theme options and preview, private search and relocking after the app enters the background.
- The theme check changed avatar, count, preview, density and unread settings, then restored them. The private row and sample preview use the same style function.
- A regression found during testing cleared screenshot protection on private Compose resume. The fix was rebuilt and the secure-window assertion passed.
- Valid custom hex input applied and updated the preview; invalid input is rejected with an inline validation error. Theme heading and dialog contrast were visually checked.
- Test authentication helpers exist only in the separate emulator test APK, not the downloadable application.

## Earlier 1.2 emulator regression (not fully repeated for 1.3)

A dedicated AOSP x86_64 emulator used synthetic SMS, contacts and MMS. No carrier messages were sent.

- Upgraded the existing 1.1 install without clearing data; messages, PIN and saved privacy rules survived schema migration 17.
- Conversation total and unread counters were displayed; opening a two-message thread cleared unread while preserving its total.
- Long-press thread actions displayed Archive, Lock & archive, Move to recycle bin and Select multiple. Moving a thread to the bin hid its messages from Inbox; restoration brought both SMS back.
- Settings exposes Contacts and the protected bin; bottom navigation is Messages / Themes / Settings. Themes opened and the Jade palette applied. Public Inbox and Themes screenshots were visually inspected.
- Bin retention choices 30, 60 and 90 days were accepted. Expiry is calculated from original deletion time.
- Repeated one-finger downward pulls remained in the public inbox. A separate emulator-only instrumentation test injected a real two-pointer pull and verified that UnlockActivity opened.
- With an enrolled synthetic fingerprint, opening the bin immediately displayed Android's biometric prompt without tapping a fingerprint button. The test fingerprint authenticated and opened the protected bin.
- SMS restore initially left stale bin text until reopening. The UI now refreshes its Realm snapshot before rendering after restoration; a repeated UI test confirmed the empty bin appears immediately.

- The separate emulator instrumentation verified MMS hiding, restoration with a byte-identical PNG attachment, retention before expiry, and deletion from both Realm and the Android provider after a synthetic 91-day expiry. Its fixture code is in the test APK only.

## Remaining device validation

Physical OnePlus 13 fingerprint sensing, Android 16 and 16 KB compatibility, real display frame pacing, carrier SMS/MMS/delivery reports, dual SIM, full legacy-feature regression, accessibility, OEM background limits, font/display combinations and real contact-photo rendering remain device checks. The emulator confirms app behavior, not physical sensor or carrier behavior. RCS is not implemented.

The bin retains shared Android provider records until expiry; it is app-level privacy, not encrypted storage. Android may defer the daily cleanup. Clearing Texto data removes its local deletion markers. See DEVICE-TESTS.md for the full acceptance checklist.
