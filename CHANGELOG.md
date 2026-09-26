# Changelog

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
