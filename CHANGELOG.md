# Changelog

## 1.6.3 — 10 October 2026

- Archived conversations stay archived when new SMS/MMS arrive or replies are sent; existing archives receive persistent number rules.
- Locked and archived conversation menus offer explicit moves to inbox/archive, locking and recycle-bin actions.
- Privacy number lists use contact cards with photos or modern fallback avatars.
- Settings are grouped into GENERAL, APPEARANCE and PRIVACY with indented options and non-clickable headings.
- A visual color ring, intensity and brightness controls replace numeric-only custom color entry.
- Four additional card finishes (Pastel, Duotone, High contrast and Bold outline) and Square/Pill shapes share the app renderer.
- Header/background artwork supports drag-and-pinch cropping and preserves image proportions.
- Three-second text selection tolerates small finger movements, with a native read-only selection sheet when an OEM refuses inline selection.
- Reviewed QUIK changes through 02542049 and removed the unused billing version declaration; overlapping upstream premium-removal work is documented separately.

## 1.6.2 — 8 October 2026

- Message text bubbles now share the selected card finish and shape, including gradients, frosted and AMOLED styles; card size and spacing also apply inside conversations.
- File/contact attachments use the same surfaces and readable foreground colors. Photo/video corners follow the selected shape and grouping; No cards removes their rounding.
- Bottom navigation, settings card sizing and remaining theme/privacy/recycle-bin dialogs follow the shared appearance settings. Modal dialogs retain an opaque background with No cards for readability.
- Expanded message/link contrast regression coverage to all eight card finishes.

## 1.6.1 — 5 October 2026

- Apply No cards to settings, drawer rows, contacts, navigation, message bubbles and file/contact attachments. Keep modal dialogs opaque and controls identifiable.
- Add Aurora gradient, frosted glass and AMOLED outline finishes using the selected accent; no real-time blur or animation overhead.
- Restore the Privacy shield icon in the drawer and Settings.
- Use rounded Material dialogs for message details, link warnings, backup, blocking, scheduled messages and other utilities.
- Replace the inherited empty update window with actual Texto release notes, visible in every device language.
- Separate manual link spans from native text movement so a deliberate three-second hold can select individual words; shorter holds retain message selection.
- Open phone links directly in the dialer using the number displayed in the message. Add an optional country-code setting; no code is added by default.
- Let Android choose the refresh rate by default; the highest-rate override is now opt-in. The reported OnePlus freeze/reboot remains unconfirmed and is not claimed fixed.

## 1.6.0 — 4 October 2026

- Hold message text for three seconds to select text inside the bubble; a shorter long press selects the message for actions. Scrolling cancels the hold; normal link taps are preserved.
- Restyle the navigation drawer with tonal cards and icons using the selected accent; add a Privacy entry.
- Open ordinary Scheduled, Message management and Backup without PIN/fingerprint prompts. Exclude locked, archived and explicitly protected numbers from ordinary scheduled lists, backups, restores and cleanup.
- Add authenticated protected-number tools in Privacy, with automatic fingerprint prompt and PIN fallback. Scope backups/restores and manual deduplication to protected numbers. Background automatic cleanup only affects ordinary messages.
- Add small/medium/large conversation cards and a No cards list option, alongside existing spacing and preview controls.
- Use gradient initial avatars and geometric portraits when a contact has no photo; keep real contact photos.
- Customize the inbox title and motto; choose separate background and header artwork with a readability overlay. Images stay on-device and can be removed or reset.
- Publish separate signed release and debug APKs. The release package app.texto.sms installs separately from app.texto.sms.debug and does not inherit its privacy settings.

## 1.5.0 — 2 October 2026

- Refresh shared cards with a WA Enhancer 1.6.0-inspired tonal finish, fine outlines, compact icon badges and tinted Settings section headers. Existing accent choices remain available.
- Keep Messages and Themes on the bottom bar; move Settings access to the three-dot menu. Settings opens the full theme editor.
- Fix Backup, Message management and Scheduled screens closing with “This conversation is private.” When locked numbers exist, these screens now request fingerprint/PIN and resume after successful authentication. Cancellation keeps their content hidden.
- Add an email/share action for saved SMS backup files and instructions for restoring downloaded email attachments.
- Exclude MMS from the inherited SMS-only backup format instead of incorrectly converting it to SMS on restore. MMS attachment backup is not supported.
- Skip identical SMS records on repeated restores and report failed backup writes instead of showing completion.

## F-Droid packaging — 1.4.0 source revision

- Build unsigned release APKs without a private keystore; remove unused Google Services and Crashlytics build plugins.
- Add Texto store descriptions, icon, screenshots and an F-Droid build recipe.
- Replace inherited QUIK store metadata and generated repository indexes.
- Validate release builds and F-Droid metadata in CI.

Submitted for [F-Droid review](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/50918) on 2 October 2026. Acceptance, official build/signing and publication remain pending.


## 1.4.0 — 27 September 2026

- About now links to Texto source and changelog.
- Settings and scheduled-message cards share conversation styling; controls and date/time pickers use the selected accent on Android 11+.
- The legacy color picker turns off wallpaper/automatic colors when a global custom color is selected.
- SMS scrolling skips attachment adapters, MMS rows reuse adapters, and unchanged avatars avoid repeated photo loads.
- Fix unreadable phone numbers, email addresses and web links in conversation bubbles with dark, AMOLED or custom colors.
- Choose message and link foreground colors together from the actual bubble background, including recycled rows and private conversations.
- Correct selection highlights and handles to use resolved colors instead of a theme attribute ID.


## 1.3.0 — 2026-09-26

### Added
- A private inbox built from the normal conversation-card adapter, with live updates, private-only search and reachable Themes / More / Lock controls.
- Live conversation and bubble previews, eight accent palettes and validated custom hex colors.
- Neutral/tinted/outlined surfaces; compact/comfortable/airy spacing; hidden or one-to-three-line previews; optional avatars and totals; unread badge/dot choices.
- Direct appearance, AMOLED and text-size controls, plus an appearance-only reset.

### Changed
- Private SMS/MMS threads inherit the same theme settings and full conversation screen as normal messages.
- Public privacy notices indicate locked content without teaching the private-entry gesture.
- Wallpaper accents now feed the shared card/bubble color path while retaining explicit per-contact overrides.

### Fixed
- Preserve secure-window protection when the normal conversation screen resumes with a private thread.
- Improve contrast for theme headings, dialogs, custom accent controls and selected navigation.

### Release status
Development APK, debug-signed. Physical OnePlus 13, carrier SMS/MMS, Android 16/16 KB and full legacy-feature regression remain device checks. RCS is not implemented.

## 1.2.0 — 2026-09-26
- ColorOS-inspired cards, message totals and unread counters.
- Long-press archive, lock and recycle-bin actions.
- Themes bottom tab; Contacts moved to Settings.
- Protected recycle bin with restore and 30/60/90-day expiry.
- More deliberate private entry and automatic biometric authentication with PIN fallback.
- Frame-coalesced header motion, list prefetch and high-refresh preference.
- Corrected stale bin contents immediately after restoration.

## 1.1.0 — 2026-09-25
- Floating navigation, reachable inbox heading and photo-capable contacts.
- Authenticated private-message sessions and number-level lock/archive persistence.
- Hidden archive entry and Addymore developer/contact credits.

## 1.0.0 — 2026-09-24
- Initial local Texto fork from QUIK 4.3.7.
- Material 3 foundation, privacy rules, spam/block controls and app branding.
- Retained upstream SMS/MMS and messaging features.
