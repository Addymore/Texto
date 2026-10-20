# Texto validation — 20 October 2026

## 1.8.0 personalization and launcher icons — 20 October 2026

- Optimized release/debug APKs and instrumentation built successfully in 6m 6s. The 19 unchanged common JVM tests have zero failures/errors. Both APKs passed signature, existing-certificate continuity and zip-alignment checks.
- Seven Android 14/API 34 emulator suites passed: all 14 launcher aliases resolve exactly one entry while retaining the real SMS activity; Settings icon/text columns align in LTR/RTL; separate bubble colors and readable text/links, font and line spacing, preset save/restore, no-cards override, three gradient directions and five border choices.
- Existing regressions passed: 108 compact-layout combinations and 1,000 recycled conversations; 480 contrast cases; 192 OEM/custom surfaces; native word selection and Copy through injected touch; message search, persistent archives and protected utilities.
- Signed 1.7.3 to 1.8.0 upgrade retained synthetic conversations and the SMS role, displayed the current changelog and left an empty crash buffer.
- Visual review confirmed aligned settings columns and found inconsistent icon-tile positioning for wrapped labels. The picker was changed to explicit image-above-label tiles; the final rebuild passed in 9m 13s and the customization instrumentation passed again. The first manual-navigation attempt encountered the expected upgrade changelog; it was dismissed before retrying. Final visual inspection confirmed centered image-above-label tiles, and selecting Samsung through the picker resolved the Samsung launcher alias.
- QUIK master was fetched and remains 02542049207bab8e467a90cc0e50d3febe2b6a8d; no newer master fixes were available.
- Physical phones, carrier SMS/MMS delivery and OEM launcher refresh timing were not tested for 1.8.0. The earlier Samsung results below apply to 1.7.3 only. Compile SDK 34 / target SDK 33 remain unchanged. Legacy AGP lint still crashes on installed SDK 37 metadata, so release lint was excluded; no clean lint result is claimed.

## 1.7.3 native selection and interaction — 14 October 2026

- Optimized release/debug APKs, instrumentation and common-test tasks passed in 8m 22s. The 19 unchanged JVM tests remain green. Both APKs passed signature, existing-certificate continuity and zip-alignment checks. The legacy SDK 37 metadata lint crash described below remains; release lint was excluded, and no clean lint result is claimed.
- Six Android 14 emulator suites passed: native word selection and Copy in the real MessagesAdapter/TightTextView, short link taps and swipe scrolling, 108 compact-layout combinations and 1,000 recycled conversations, 480 contrast cases, 192 OEM/custom SMS/MMS surfaces, current-version changelog, icons, search, archive and protected utility behavior.
- Physical Samsung A57 (SM-A576B), Android 17/API 37: a separate debug-only synthetic screen passed an injected native finger long press, visible native handles and Copy toolbar, a real tap on Copy with matching clipboard contents, short link taps and message-body swipe scrolling. The test used the production TightTextView, selection helper and bubble renderer, but did not read real messages or exercise carrier delivery. Production Texto 1.7.2, its SMS role and SMS/contact permissions remained unchanged; the clipboard was restored.
- Early physical attempts ran before the test window received input focus. The final harness waits for focus, identifies finger input explicitly and taps the Samsung Copy label rather than assuming that the label itself owns the click listener. Test-only rebuilds passed; the final application APKs were unchanged.
- The signed release upgraded 1.7.2 without clearing data, retained synthetic messages and the SMS role, displayed the 1.7.3 changelog and left an empty crash buffer.
- The custom three-second timer and accessibility-based selection activation were removed. Native selectable TextViews use Android's range-capable movement method; short link taps are dispatched without consuming the native long press or drag gesture. Text selection retains the rounded highlight, while whole-message actions remain available separately.
- Avoidable typeface assignments, per-draw corner-effect allocations and redundant Contacts header layouts were removed. Contacts now refreshes its theme on return and honors configured bottom navigation; section headings use the shared readable foreground. These changes are not a measured frame-rate guarantee.
- QUIK master was fetched and remains 02542049207bab8e467a90cc0e50d3febe2b6a8d. No newer master fixes were available to import.
- Carrier SMS/MMS delivery, OnePlus hardware and biometric behavior were not exercised. Compile SDK 34 / target SDK 33 remain unchanged.

## 1.7.2 cross-screen theme consistency — 14 October 2026

- Final optimized release/debug APKs, instrumentation and common-test task passed in 7m 22s. The 19 unchanged JVM tests remain green (Gradle up-to-date reports). Both APK signatures, existing certificate identities and zip alignment passed.
- Five emulator suites passed on Android 14: 192 OEM/custom SMS and attachment surface cases; all eight saved-profile fallbacks; Settings icons; repeated current-version About changelog; 480 custom color/link contrast cases; 108 finish/size/spacing combinations and 1,000 recycled conversations; actual message selection, search, archive and privacy utility regressions.
- The test harness was corrected after AGP replaced the first runner registration and a theme change recreated the fixture Activity. The final test-only rebuild passed in 52s; the app APKs were unchanged. Theme rendering now uses an independent themed context.
- The signed release upgraded 1.7.1 without clearing data, retained synthetic conversations and the SMS role, displayed 1.7.2 release notes and left an empty crash buffer. Settings and the About version changelog were opened through production navigation and visually inspected.
- The legacy AGP 8.2.2 lint analyzer crashes while reading the newly installed SDK 37 metadata (`NumberFormatException: "37.0"`). Release lint tasks are excluded for this build; no clean lint result is claimed. Compilation, release optimization, JVM tests and emulator regression checks remain enabled.
- QUIK master was rechecked: 02542049207bab8e467a90cc0e50d3febe2b6a8d, unchanged. The latest three PR Actions runs require approval; no unreviewed workflow artifacts were imported.
- Physical Samsung/OnePlus, carrier SMS/MMS and biometric hardware checks remain unverified. Compile SDK 34 / target SDK 33 remain unchanged.

## 1.7.1 inline selection, OEM profiles and image memory — 14 October 2026

- Final optimized release/debug builds, instrumentation build and common-test task passed in 6m 55s. The 19 unchanged JVM test reports have zero failures/errors. Both APKs passed signature, existing-certificate continuity and zip-alignment checks. The inherited non-aborting lint configuration remains; no clean full lint result is claimed.
- The signed release upgraded the existing 1.7.0 emulator installation without clearing data. The 1.7.1 changelog appeared, synthetic conversations remained, the production SMS role was confirmed and the crash buffer was empty. A real 3.3-second injected touch selected a word inside its original bubble; dragging a native handle expanded the range across lines. A shorter hold selected the whole message with a rounded bubble-shaped outline.
- All four suites passed against the final debug app on Android 14. The contrast fixture initially compared an active OEM bubble with a custom card palette; it now isolates custom finishes and restores the prior OEM preference. That test-only rebuild passed in 37s. The app APKs were unchanged by this correction.
- Real MessagesAdapter/TightTextView/RecyclerView tests select and copy a word only after three seconds, preserve the range on release, show the native Copy toolbar, and copy a longer passage. Short holds retain message selection and scroll movement cancels text selection. Whole-message and text-range highlights use the new shaped rendering.
- All 108 finish/size/spacing combinations and 1,000 synthetic recycled conversations passed. The standard row remains 70 dp. Added narrow-screen checks cover enlarged fonts, unread counts/dots, simultaneous pinned/scheduled indicators, zero to three preview lines, hidden counts and left-to-right/right-to-left layouts. Preview bounds stay outside the reserved indicator column.
- Every OEM profile applied successfully, outgoing text contrast passed against its rendered bubble, incoming/outgoing surfaces differ, and restoring the previous theme passed. Default themes remain unchanged until Apply. Samsung and ColorOS profiles were also inspected through the actual Themes UI.
- All 480 custom message/link contrast cases passed across twelve finishes, light/dark/AMOLED, incoming/outgoing, custom accents and recycled views, plus Settings and the scheduling date picker.
- Bounded system-avatar tests loaded circular and uncropped images, then released Glide's memory cache and verified that the independently owned bitmap remained readable and correctly colored. Missing/null photos return the fallback path. This verifies ownership and sizing, not a measured long-running device memory benchmark.
- Search/privacy/archive and utility regressions passed: private, blocked and trashed results remain excluded; archive rules persist; ordinary utilities open without authentication; protected utilities require verification and close on cancellation. Title/motto, artwork/crop, color ring and no-card checks passed.
- No physical Samsung A57 or OnePlus device was visible over USB during these checks. API 37, carrier SMS/MMS, fingerprint hardware and physical frame pacing remain unverified. Compile SDK 34 and target SDK 33 remain unchanged. The bundled arm64 Realm library has 4 KB ELF LOAD alignment; 16 KB native support is not established. See [Android compatibility](docs/ANDROID_17.md).
- QUIK master remains 02542049; the latest reviewed Actions require approval and yielded no new reviewed master fixes. F-Droid and Google Play publication remain separate pending processes. Screenshots contain only synthetic messages.

## 1.7.0 compact conversations and message tools — 14 October 2026

- Final optimized release/debug builds and common-test task passed in 3 minutes 31 seconds. All 19 unchanged common JVM test reports have zero failures/errors (up-to-date results). Both APK signatures, their existing release/debug certificate fingerprints and 4-byte alignment verified. The inherited non-aborting lint configuration still reports 81 translation errors; a clean full Android lint run is not claimed.
- The signed production APK upgraded the existing 1.6.4 Android 14 installation without clearing app data. Version 1.7.0 release notes appeared, the synthetic conversations remained, the production SMS role was confirmed and the crash buffer was empty. A real three-second hold also displayed native selection handles and the Copy menu in this optimized production build.
- The final debug and instrumentation APKs passed all four Android 14 emulator suites. Selection verification now requires the native Copy toolbar to be visible, not just a highlighted range. An actual three-second touchscreen hold opened the controls immediately, and dragging a handle expanded the selection across words. Word and longer-passage copying passed in the real message adapter. The final debug and test rebuild took 2 minutes 14 seconds.
- Compact-layout coverage passed all 108 finish/size/spacing combinations, larger text and unread badges, and 1,000 synthetic conversations. The standard sample row measured 70 dp, down from 77 dp in 1.6.4, without reducing fonts or preview lines. Unchanged binds retain backgrounds without requesting layout; the unchanged bottom bar reuses its buttons. These checks do not establish physical OnePlus frame pacing.
- Search returned individual public SMS/MMS matches newest first while excluding locked, blocked and trashed fixtures. Manual UI checks confirmed highlighted message previews, full-text preview, and hidden bottom navigation during search. Results are capped at the newest 500 matches.
- Phone links offered Open in dialer or Copy number. Confirming the first action produced ACTION_DIAL, with as-written numbers and optional country-code behavior preserved. The phone-action dialog inherited a protected parent's secure-window flag. A separate real Activity transition confirmed that the secure text-selection dialog closes when its Activity pauses.
- Optional OEM presets passed apply/switch/restore checks, including exact restoration of previously absent preferences. The navigation configuration caps shortcuts at five, and five labels including Backup & restore were visually checked on the emulator. Manual checks confirmed that browsing and cancelling a preset changes no appearance preferences, and the Show bottom bar switch hides the inbox bar.
- All 480 message/link contrast cases passed across twelve finishes, light/dark/AMOLED, incoming/outgoing, custom accents and recycled views, plus Settings and the scheduling picker.
- Utility/privacy regression passed: ordinary Scheduled, Backup and Message management open without authentication; protected utilities stay hidden until PIN verification; cancelling authentication closes the protected utility. Persistent archive, number rules, scroll-cancelled selection, title/motto, image crop, color ring and no-card rendering also passed.
- QUIK master and Actions were rechecked on 14 October: master remains 02542049 and the latest three PR workflow runs require approval. No unreviewed artifacts were imported.
- Screenshots use synthetic emulator messages. Physical OnePlus performance, carrier SMS/MMS, fingerprint hardware and full email/backup round trips remain unverified. Android 17/API 37 and manufacturer features are not certified: compile SDK 34 and target SDK 33 remain unchanged; see docs/ANDROID_17.md. F-Droid and Google Play are separate pending review processes.

## 1.6.4 compact layouts and scrolling — 13 October 2026

- Final optimized release and debug builds passed in 15m 43s; the final test-harness-only rebuild passed in 1m 26s. The 19 unchanged common JVM tests have zero failures/errors (Gradle up-to-date results). Release/debug APK signatures, existing certificate fingerprints and 4-byte alignment passed.
- Android 14 compact-layout regression passed all 108 finish/size/spacing combinations. The standard sample row measured 77 dp, larger text expanded naturally, and the unread badge accommodated larger text without clipping. Every settings row and click listener survived regrouping into five sections.
- A real conversation adapter displayed and recycled 1,000 synthetic conversations. Reapplying unchanged conversation styling reused its drawable without requesting layout; counts and selection still updated. This is not a measured physical-device frame-rate claim.
- The real message adapter selected and copied an individual word after a three-second hold. Added assertions verify cached link reuse, an edited message's new phone number, and removal of stale links when the row becomes plain text. Persistent archive and protected-number assertions also passed.
- All 480 contrast cases passed on the final APK, covering 12 finishes, light/dark/AMOLED, incoming/outgoing text, links, custom accents, recycled views, Settings cards and the scheduling date picker. The harness now yields between configuration batches: an earlier unbatched run caused a background-service ANR and was discarded. Final results were accepted only with an explicit PASS, not merely the instrumentation exit code.
- Privacy/utility regression passed: ordinary utilities open without a PIN, protected tools remain gated, cancellation closes protected utilities, and no-card transparency plus visible selection are checked by rendered pixels. Title, motto, artwork, crop, hue ring and gesture checks also passed.
- The signed production APK upgraded the existing Android 14 emulator installation without clearing app data. Texto 1.6.4 release notes appeared, the production SMS role was confirmed, the synthetic inbox loaded and the crash buffer was empty. Both release/debug packages retain their respective signing identities.
- Manual UI checks confirmed the five settings groups and working moved Dialing country code and Delayed sending dialogs.
- The initial workstation build ran out of native memory while the emulator was running. Building and testing sequentially succeeded. This was a development-computer resource failure, not an app crash.
- Release lint still reports the same 81 inherited translation errors as 1.6.3 under the existing non-aborting configuration; a clean lint result is not claimed.
- QUIK master and Actions were reviewed again: master is unchanged at 02542049. No unreviewed upstream PR artifacts were imported.
- Physical OnePlus frame pacing, carrier SMS/MMS, fingerprint hardware, full backup/restore and official F-Droid/Google Play publication remain separate, unverified checks.

## 1.6.3 archive and appearance regression checks — 10 October 2026

- Debug and optimized release builds passed in the final Gradle run (7m 22s). Both APKs passed signature/certificate continuity and zip alignment checks. The signed release upgraded the existing Android 14 emulator installation, displayed the 1.6.3 release notes and opened the synthetic inbox with no crash in the emulator crash buffer.
- Final debug and instrumentation builds passed. The 19 unchanged common JVM tests remain passing (Gradle up-to-date results).
- Android 14 synthetic-emulator regression: persistent number archiving survives automatic unarchive attempts; explicit unarchive works and locked rules remain protected. Receive/send call sites use the automatic path. New/recreated conversations also apply saved archive rules.
- The actual MessagesAdapter, TightTextView and RecyclerView select an individual word after three seconds, preserve selection on release and copy only that word. Earlier overlapping instrumentation was discarded; isolated final runs passed. Physical OnePlus native/fallback selection remains unverified.
- All 480 contrast cases passed across 12 finishes: incoming/outgoing messages, links, light/dark/AMOLED, custom accents and recycled views, plus settings and scheduled picker checks.
- Utility/privacy regression passed: ordinary utilities open freely, protected utilities remain hidden until PIN authentication, cancellation closes protected screens, scroll movement cancels text selection, custom title/motto/artwork load, crop preserves a synthetic circle's proportions, hue-ring touch selects the expected hue, and card-size/no-card checks pass.
- Manual emulator checks confirmed the indented settings groups, working Theme navigation, archive action menu and move to inbox, color-ring dialog, saved artwork crop after dragging, and privacy number cards through the normal PIN flow.
- F-Droid metadata lint and source scan passed on GitHub Actions run 38085383901, job 114310686687. Existing submission !50918 now targets 1.6.3/code 2320; GitLab pipeline 2934603858 created zero jobs because identity verification is required. Official F-Droid CI/inclusion remains pending.
- Physical OnePlus frame pacing, carrier SMS/MMS, fingerprint hardware, real email delivery and a complete backup/restore round trip remain unverified. The earlier reported system reboot has no confirmed cause or claimed fix. F-Droid review and Google Play publication are separate pending processes.


## 1.6.2 theme consistency checks

- Final release and debug builds passed in 14m 28s; all 19 common JVM tests remain passing (Gradle up-to-date). Both APK signatures, existing certificates and 4-byte alignment verified. Version is 1.6.2, code 2310. The signed release upgraded the emulator installation and launched successfully with the SMS role; no crash was recorded in the emulator crash buffer.

- Debug and instrumentation builds passed. All 320 message/link contrast cases passed across eight finishes, light/dark/AMOLED, incoming/outgoing, custom accents and recycled views. Settings card tint and scheduled-picker accent checks passed.
- The real adapter three-second selection/Copy regression and privacy/utility suite passed again.
- The debug APK retains the existing development certificate and passes alignment verification. The theme regression suites ran on the development build before the release version bump. Physical OnePlus rendering/performance and carrier traffic were not tested.

## 1.6.1 automated and emulator checks

- Final optimized release build passed in 8m 35s. All 19 common JVM tests remain passing (Gradle up-to-date results). Both downloadable APKs passed signature and 4-byte alignment verification and retain their existing release/debug certificates. Version code is 2300; packages remain `app.texto.sms` and `app.texto.sms.debug`.
- The final signed release APK installed over the existing production-package emulator installation and launched successfully. After granting the default-SMS role, the synthetic inbox loaded with no crash recorded in the emulator crash buffer.
- Final debug and instrumentation builds passed. A new regression uses the real MessagesAdapter, TightTextView and RecyclerView: text is not selected at 1.6 seconds, an individual word is selected after three seconds, selection survives finger release, and native Copy copies only that word. A separate synthetic conversation was also checked with an actual 3.3-second injected touch; native selection handles and the Copy toolbar were visible.
- Direct phone-link activation produced ACTION_DIAL with the displayed number. Default empty country code, optional country code, international numbers and special-number handling passed the regression checks.
- No cards passed transparent settings and message-bubble checks. All new card finishes were exercised. Flat settings/drawer, the Privacy shield icon, native selection and rounded Jade-accent release notes were visually checked. Repository screenshots contain only synthetic messages.
- The privacy and utility regression passed again: ordinary Backup, Message management and Scheduled open without PIN; protected tools remain hidden until verification; cancellation closes protected tools. Number rules, mixed groups, archived/locked threads, title/motto, artwork and card-size checks passed.
- All 40 message contrast cases passed again, including incoming/outgoing, light/dark/AMOLED, custom accents, phone/web and confirmation links, recycled views, Settings cards and scheduled date-picker checks.
- The actual 1.5.0 debug APK was upgraded to the 1.6.1 candidate on the Android 14 emulator without clearing app data. All 43 synthetic SMS rows, privacy preferences and the emulator boot ID were unchanged. Texto release notes appeared. Missing changelog preferences were also tested separately to verify first-run notes.
- Read-only OnePlus diagnostics did not establish the cause of the reported freeze/reboot. The retained system restart identified during investigation did not match the time reported by the user. This release makes no claim that the physical-phone reboot cause is fixed. Android controls refresh rate by default; the explicit highest-rate option remains available.
- Physical OnePlus frame pacing, carrier SMS/MMS, actual fingerprint sensing, real email delivery and a complete backup/restore round trip remain unverified. Existing translation lint issues remain; a clean full lint result is not claimed. F-Droid's submission still targets 1.6.0 and review is pending; Google Play publication is pending.

See [APK signing and upgrade notes](docs/RELEASE-SIGNING.md) before switching between release, debug and F-Droid packages.

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
- F-Droid MR 50918 was updated to the 1.6.0 (2290) source recipe on 4 October 2026. Metadata lint and source scanning passed: https://github.com/Addymore/Texto/actions/runs/37233479899/job/111527872393 . Official F-Droid CI/review and Google Play publication remain pending.

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
