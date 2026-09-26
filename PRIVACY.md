# Texto privacy notes

Texto is a local SMS/MMS client. It needs access to messages to display, send, receive, search and restore them; contacts support names and photos. Carrier MMS uses network access. Optional external links open their named services.

Locked-number rules protect content within Texto. Authentication uses an app PIN or Android strong biometrics. Biometric templates remain with Android. The shared Android Telephony provider and inherited Realm database are not encrypted by this feature. Other SMS-authorized apps, device administrators/root and exported backups are outside its protection.

Recycle-bin deletion preserves provider rows and MMS parts until expiry so they can be restored. Retention is measured from deletion time. Cleanup runs on startup and daily, subject to Android scheduling. Clearing app data removes local rule/deletion markers.

Do not include real numbers, message contents, PINs, backups or credentials in public issue reports. Security concerns can be sent privately to [Addymore on Telegram](https://t.me/addymore). This document describes the development build's data handling; it is not a claim of independent security certification.
