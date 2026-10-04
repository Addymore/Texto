# Texto privacy notes

Texto is a local SMS/MMS client. It needs access to messages to display, send, receive, search and restore them; contacts support names and photos. Carrier MMS uses network access. Optional external links open their named services.

Locked-number rules protect content within Texto. Authentication uses an app PIN or Android strong biometrics. Biometric templates remain with Android. The shared Android Telephony provider and inherited Realm database are not encrypted by this feature. Other SMS-authorized apps, device administrators/root and exported backups are outside its protection.

Recycle-bin deletion preserves provider rows and MMS parts until expiry so they can be restored. Retention is measured from deletion time. Cleanup runs on startup and daily, subject to Android scheduling. Clearing app data removes local rule/deletion markers.

Ordinary backup, restore, scheduling lists and cleanup exclude locked, archived and explicitly protected numbers. Authenticated Privacy tools operate on that protected scope. Automatic cleanup only processes ordinary messages. User-selected backup files are readable SMS text, not encrypted archives. SMS-only backups omit MMS attachments and Texto privacy settings.

Custom background/header images are selected through Android's document picker and displayed locally with retained read access. They are not uploaded by Texto or included in SMS backups.

Do not include real numbers, message contents, PINs, backups or credentials in public issue reports. Security concerns can be sent privately to [Addymore on Telegram](https://t.me/addymore). This document describes Texto's data handling; it is not a claim of independent security certification.
