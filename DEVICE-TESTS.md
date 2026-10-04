# Texto device acceptance tests

The local JVM rule tests do not replace these device checks. Use test numbers and consented test messages. Do not replace a primary SMS app before validating backup/restore.

1. Install the debug APK alongside QUIK. Select Texto as the default SMS app; allow SMS, Contacts and notification permissions. Confirm initial message sync.
2. Send/receive SMS, multipart Unicode SMS, group MMS, photo MMS, voice attachment and another file attachment. Test both SIMs and airplane-mode failures/retry. Carrier delivery reports must show two ticks only after delivery; there are no SMS read receipts.
3. Create a 6–12 digit PIN, then add a number to Locked & archived. Confirm the existing thread leaves Inbox. Receive SMS and MMS from that number: the thread stays archived, no preview appears, and no shortcut or widget exposes it.
4. Put Texto in the background, return, rotate the screen and restart the process. Try the archived thread through search, a share intent, an old shortcut and quick reply. Public lists must exclude private threads and direct intents must be denied. Only pulling down with two fingers in Messages opens PIN/strong biometric authentication. Cancel and verify content stays hidden. Test five wrong PINs, persistent cooldown, correct PIN and biometric fallback.
5. Long-press Texto to open Archive; it must not appear in bottom navigation. Add an Archive-only number. New SMS/MMS stays in Archive. Remove the rule, manually unarchive, then confirm normal inbox behavior. Test a group containing a locked/archived participant.
6. Add exact blocked numbers, a prefix and a spam phrase; check quarantine without deleting messages. Confirm trusted entries override Texto spam rules, but not privacy. Remove the rule and unblock the conversation to restore it. Test alphanumeric senders and national/international formats with the correct SIM country.
7. Verify contact directory search, profile actions, photos/media, saved contacts and conversation colors. Test light, dark, AMOLED, large fonts, landscape, wallpaper colors on Android 12+, and small displays.
8. Regression: scheduled sends, delayed sends/cancel, backup/restore, text-to-speech, speech-to-text, emoji reactions, search, pinned conversations, swipe actions, notification quick reply on nonprotected threads, MMS APN configuration and dual SIM.

## Privacy boundaries

- Ordinary messages open freely. Pull down with two fingers in Messages to authenticate into Private messages. Returning to Messages or leaving the app ends the session. Direct links cannot bypass the gate. Legacy backup and scheduled-message screens require a private session while locks exist. The app PIN is separate from the device PIN.
- PIN uses a random salt, PBKDF2 and persisted retry delays. Strong Android biometrics can unlock a session; biometric templates stay with Android.
- SMS/MMS remains in Android's shared Telephony provider and the inherited Realm store. This is **not** an encrypted message vault and cannot protect against another SMS-authorized app, root, an unlocked device administrator or exported backups. A person able to enroll device biometrics can use them.
- Background notification reply/respond-via-message is disabled for locked numbers. Already scheduled sends remain scheduled.
- New number/phrase rules quarantine; existing inherited content-filter/drop settings may delete messages if the user enables them.
- After removing a rule, previously archived/blocked conversations remain there until manually restored.
- New Texto screens are currently English. Upstream translations remain for inherited screens.

## Platform boundaries

- Minimum Android 6 (API 23), compile API 34, target API 33 retained from QUIK. This is a development fork, not a Play Store-ready release.
- Physical carrier SMS/MMS, Android 16, 16 KB memory-page devices, accessibility, OEM background limits and fingerprint sensors require device validation.
- RCS is not implemented: ordinary third-party installs cannot acquire Android's privileged IMS single-registration permissions. See https://source.android.com/docs/core/connect/ims-single-registration.

## Texto 1.2 acceptance checks

1. Upgrade from 1.1 without clearing data; verify PIN, archived/locked rules and existing SMS/MMS.
2. Receive several messages in one thread, verify total and unread counts, open it and verify unread clears while total stays unchanged.
3. Long-press to archive, lock and move threads to the bin; verify multi-select still works. New messages from locked senders remain locked.
4. Delete one SMS and one photo MMS. Neither appears in public search, conversation media or backup. Open the bin via Settings, reject a wrong PIN, unlock with the same PIN/strong biometric and restore. Confirm MMS attachments survive.
5. Background the bin, return, cancel authentication and try deep links. Private content must remain hidden. Force-stop/restart and resync messages; deleted items must stay deleted in Texto.
6. Choose 30, 60 and 90 days. On a disposable test install, seed deletion timestamps just below/at each boundary and run cleanup; only expired entries should disappear from both Realm and Telephony. Confirm provider failures retain items for retry.
7. A new incoming SMS in a wholly deleted thread should show only the new live message; older deleted messages stay in the bin until restored.
8. Exercise Themes palettes, spacing, card shape, bubble styles and motion settings. Check light/dark, maximum font size, landscape and your OnePlus 13 refresh-rate settings. Profile native scrolling on the actual phone before claiming ColorOS-equivalent smoothness.

The bin covers received/sent SMS and MMS routed through normal message/conversation deletion, old-message cleanup and deduplication. Unsent drafts and scheduled-message definitions are not recoverable SMS/MMS bin items. The provider retains deleted content until expiry; the bin is not encrypted storage.

## Two-finger privacy entry

- One-finger swipes and normal flings must never open authentication. Both fingers must move downward at the top of Messages; stationary second fingers, short pulls, cancelled gestures and reversed pulls must not unlock.
- With an enrolled strong biometric, the Android authentication prompt must open automatically. Use PIN, cancel, failed scans, rotation, backgrounding and unavailable/locked-out biometrics must retain PIN fallback without revealing content. Verify on the physical OnePlus 13.

## Texto 1.3 acceptance checks

1. Open the private list after authentication. Compare its avatars, timestamps, cards, previews and counters with Inbox. Open a private SMS/MMS thread and confirm it uses the normal thread layout and bubble theme.
2. In Themes, change the palette/custom color, card finish, shape, density, preview length, unread indicator, avatar and count visibility. Verify the live sample, public inbox and private list agree; return from Themes without exposing private content in public lists.
3. Search inside the private list, then confirm the same private text remains absent from public search. Background or lock Texto and confirm private screens cannot reopen without fresh authentication.
4. Check dark/light/system appearance, AMOLED, wallpaper colors and text sizes. Reject invalid custom hex colors. Reset appearance and confirm messages, PIN, privacy rules and retention are unchanged.
5. Confirm public lock confirmations, denied access, protection descriptions and scrolling tips do not tell an observer how to enter private messages.

## Texto 1.4 acceptance

- Read linked phone numbers, URLs and email addresses in sent/received and private conversations using light, dark, AMOLED and custom accents; verify link confirmation and copy/select behavior.
- Pick Jade, Rose and a custom hex color. Reopen Settings, Themes, Contacts, scheduled messages and privacy settings. Verify controls follow the accent, cards follow shape/finish, and toolbar text remains readable.
- Open both scheduled-message date/time pickers and scheduled dark-mode time pickers. Verify the accent and confirm the chosen date/time remains correct.
- Open About and verify Texto source/changelog destinations; upstream credits remain in the repository.
- Fling a long conversation containing SMS, photos, files, audio and contact cards. Verify attachments open the right message after repeated scrolling, playback controls still work, and measure frame pacing on OnePlus 13.
- Exact custom platform-dialog accents use Android 11+ resource loaders; older Android needs separate visual regression.

## Texto 1.6.1 acceptance checks

1. Upgrade the same debug package from 1.5/1.6 without clearing data. Confirm message counts, privacy rules and selected accent remain. The update dialog must contain actual Texto notes, including on a non-English device.
2. Choose No cards and inspect Inbox, Archive, private conversations, Settings, contact rows, drawer, file/contact attachments and the bottom bar. Dialogs and action controls remain readable. Switch through all finishes and back; check light, dark and AMOLED themes.
3. Hold a word in an incoming and outgoing message for three seconds without moving. Release, adjust both selection handles, copy only a few words and paste into the draft. A shorter hold selects the message for actions; scrolling cancels the text hold.
4. Tap a local phone number in a message. The dialer must open with the displayed number and no web warning. Configure a country code in Settings and repeat; international numbers and emergency/service codes must remain unchanged. Web-link confirmation must still follow its own preference.
5. Check Privacy icons and rounded themed message-details, scheduled, backup, blocking, country-code and changelog dialogs.
6. For any freeze/reboot, record the exact time, Android build and installed Texto version, then capture Android crash/ANR/reboot diagnostics promptly. An unrelated retained crash does not establish the cause. The 1.6.1 changes do not claim a confirmed reboot fix.
