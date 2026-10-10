# Texto features

## Messaging

- SMS and carrier MMS, group conversations and attachments.
- Dual-SIM selection, delayed sending, scheduled messages, drafts, search, pinning, conversation renaming, reactions, speech and notification quick reply inherited from QUIK.
- Sent, delivered (when the carrier reports delivery) and received indicators. No SMS read receipts or RCS claim.
- Hold message text for three seconds to select text inside the bubble; a shorter long press selects the message for actions. Scrolling cancels the hold; normal link taps are preserved.
- Phone links open the dialer directly, preserving the displayed number. An optional country-code setting replaces a local leading zero only when configured; international and short service numbers are preserved.
- Conversation message totals and unread badges or dots.
- Contact names/photos, gradient initials and geometric fallback avatars, contact profiles and contact access from Settings.

## Layout and themes

- Reachable collapsible header; Messages/Themes bottom navigation; Settings in the overflow menu.
- Accent-aware drawer and settings cards, eight palettes, custom hex accents and supported Android wallpaper colors.
- System/light/dark/scheduled appearance, AMOLED and text-size controls.
- Small/medium/large conversation card size; soft/round/minimal shape; tonal/neutral/tinted/outlined, Aurora gradient, frosted glass and AMOLED outline finishes. No cards removes decorative card surfaces across lists, Settings, navigation, bubbles and file/contact attachments.
- Compact/comfortable/airy spacing, zero-to-three preview lines, optional avatars/totals and unread dots/counts.
- Shared public/private conversation layouts and message bubble themes.
- Custom inbox title and motto; separate background image and header artwork with a readability overlay; removal/reset controls.
- Rounded accent-aware dialogs and actual Texto update notes in every locale.
- System-controlled refresh rate by default, optional high-refresh preference and reduced motion. Actual frame pacing depends on the device.

## Privacy and message management

- Persistent number-based locks and archive rules apply to future SMS/MMS.
- PIN and Android strong biometrics; private notifications suppress content; protected messages excluded from public search, widgets and shortcuts.
- Ordinary Scheduled, Backup and Message management open without authentication.
- Locked/archived numbers and optional additional protected numbers are excluded from ordinary utility scopes.
- Privacy offers authenticated scheduled-message management, SMS backup/restore and manual deduplication for protected numbers, plus explicit cleanup of protected messages older than 30/60/90 days.
- Background automatic cleanup applies only to ordinary messages. Protected messages remain available for explicit management while authenticated.
- Protected recycle bin retains messages/MMS parts for restoration; 30/60/90-day expiry options.
- Exact-number, prefix and phrase spam rules; trusted exceptions; blocklist and blocked-message quarantine.
- Archive remains available through a long press on the inbox title. Private access remains discreet.

## Backups and distribution

- User-selected SMS JSON backup folders and restores; repeated restores skip identical SMS records.
- User-initiated email/share chooser for a saved backup; restore a downloaded email attachment.
- Backups are readable SMS text. They do not include MMS attachments, PINs, privacy rules or themes.
- Separate release/debug APKs, source archives, checksums and changelog on GitHub.
- GPL source derived from QUIK/QKSMS; upstream attribution retained.

## Limits

Private space is app-level access control, not encryption of Android's shared SMS/MMS provider. Other authorized SMS apps can access provider data. Switching package/signature does not migrate app-private settings. Carrier behavior, real email delivery, complete backup/restore round trips and physical OnePlus performance require device validation. Google Play and F-Droid publication are separate review processes; see VALIDATION.md and the release notes for the actual tested state.

### Consistent message surfaces (1.6.2)

Message bubbles share card finish, shape, size and spacing with the app. File/contact attachments share themed surfaces; photo/video thumbnails follow corner and grouping choices. Navigation and privacy dialogs use the shared renderer. No cards removes decorative surfaces; modal dialogs remain opaque for readability.

### Archive, privacy and appearance controls (1.6.3)

- Archived conversations stay archived when new SMS/MMS arrive or replies are sent; existing archives receive persistent number rules.
- Locked and archived conversation menus offer explicit moves to inbox/archive, locking and recycle-bin actions.
- Privacy number lists use contact cards with photos or modern fallback avatars.
- Settings are grouped into GENERAL, APPEARANCE and PRIVACY with indented options and non-clickable headings.
- A visual color ring, intensity and brightness controls replace numeric-only custom color entry.
- Four additional card finishes (Pastel, Duotone, High contrast and Bold outline) and Square/Pill shapes share the app renderer.
- Header/background artwork supports drag-and-pinch cropping and preserves image proportions.
- Three-second text selection tolerates small finger movements, with a native read-only selection sheet when an OEM refuses inline selection.
- Reviewed QUIK changes through 02542049 and removed the unused billing version declaration; overlapping upstream premium-removal work is documented separately.
