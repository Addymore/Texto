# Texto features

## Messaging

- SMS and carrier MMS, group conversations and attachments.
- Dual-SIM selection, delayed sending, scheduled messages, drafts, search, pinning, conversation renaming, reactions, speech and notification quick reply inherited from QUIK.
- Sent, delivered (when the carrier reports delivery) and received indicators. No SMS read receipts or RCS claim.
- Use the normal Android long press on message text, as in QUIK, then drag native handles to select words or paragraphs. “Select message” in the selection toolbar and holds outside the body retain whole-message actions.
- Phone links offer Open in dialer or Copy number, preserving the displayed number. An optional country-code setting replaces a local leading zero only when configured; international and short service numbers are preserved.
- Conversation message totals and unread badges or dots. A reserved indicator column keeps unread, pinned and scheduled indicators clear of previews and totals, including right-to-left layouts.
- Contact names/photos, gradient initials and geometric fallback avatars, contact profiles and contact access from Settings.

## Layout and themes

- Reachable collapsible header; optional bottom navigation with up to five shortcuts (Messages/Themes by default); Settings in the overflow menu.
- Accent-aware drawer and settings cards, eight palettes, a visual color ring and supported Android wallpaper colors.
- System/light/dark/scheduled appearance, AMOLED and text-size controls.
- Small/medium/large compact card sizes; Soft, Round, Minimal, Square and Pill shapes. Twelve finishes: Tonal, Neutral, Accent tint, Outlined, Aurora gradient, Frosted glass, AMOLED outline, Pastel, Two-tone gradient, Bold outline, High contrast and No cards. No cards removes decorative card surfaces across lists, Settings, navigation, bubbles and file/contact attachments.
- Compact cards, smaller avatars and badges, and reduced message/settings padding in 1.6.4.
- General, Inbox & Contacts, SMS & MMS, Appearance and Privacy settings groups.
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
- Native Android text selection replaces the custom three-second gesture in 1.8.0. Full-text search previews also offer a separate read-only native text selector.
- Reviewed QUIK changes through 02542049 and removed the unused billing version declaration; overlapping upstream premium-removal work is documented separately.

## 1.7.0 additions

- Native message selection controls; 1.7.1 moves the three-second gesture directly into the original bubble.
- Phone-number actions: open the dialer or copy the number.
- Individual SMS/MMS search cards with highlighted snippets, dates, full preview and text selection; newest 500 matches.
- Hide the bottom bar or choose up to five of Messages, Themes, Contacts, Scheduled, Settings and Backup. Search always hides the bar.
- Eight optional visual presets across the shared card and message renderer. Presets are inspired styling, not OEM system features.

Current themes are kept on upgrade. Optional OEM presets require Apply and offer Restore my previous theme. Conversation padding, avatars and row gaps are smaller, with unchanged font sizes and preview settings. Unchanged text avoids repeated layout, and returning to the inbox no longer forces a full redraw.

### Inline selection and complete visual profiles (1.7.1)

In 1.7.3, use the standard Android long press to select the word under your finger, then drag native handles and use Copy for longer passages. Whole-message actions remain available from the selection toolbar and outside the text; their highlight follows the bubble shape.

Optional OEM-inspired profiles now coordinate accent palettes, header scale/alignment/typography, conversation-list surfaces, preview density, navigation and distinct incoming/outgoing bubbles. Apply is explicit; Restore returns the previous theme, including its accent and dynamic-color setting. Custom header text, images, privacy and navigation choices remain yours. These are Texto visual interpretations, not OEM applications or manufacturer integrations.

### Android compatibility and image memory (1.7.1)

- Bounded notification, widget and sharing-shortcut avatars now release image requests and retain safe bitmap copies, reducing retained image resources on Android 15–17 and older supported versions. Full API 37 migration and physical-device certification remain pending.

See [Android 15–17 compatibility](ANDROID_17.md) for platform scope and outstanding physical-device checks.

### Theme consistency (1.7.2)

OEM palettes and custom finishes carry through SMS bodies and MMS attachment surfaces. Older saved OEM presets remain recognized. Blocked, scheduled, contact and recycle-bin lists share theme surfaces and readable foregrounds; settings and About include icons. The About version row reopens the installed release changelog.

## Personalization added in 1.8.0

- 14 launcher choices: Texto original and 13 OEM-inspired interpretations, with adaptive and Android themed-icon variants. Icon selection is independent of visual presets and leaves the real SMS activity enabled.
- Separate sent/received text-bubble colors with automatic readable text and link contrast. No-card mode takes priority.
- Theme, sans-serif, serif and monospace message fonts; normal, relaxed and spacious line spacing.
- Automatic, none, fine, medium or bold card borders; diagonal, vertical or horizontal gradients.
- Aligned Settings icon/text columns in left-to-right and right-to-left layouts.
- Live previews and reversible OEM presets retain the new options.
