# Android 17 compatibility review — 13 October 2026

Texto 1.7.0 retains compile SDK 34 and target SDK 33. It is not certified on API 37 or physical One UI 9 / ColorOS 17 devices. The installed SDK platforms are 34 and 36; the regression emulator is API 34. A full target-SDK migration (including native libraries, background work, permissions and edge-to-edge behavior) remains required.

Changes in this release:

- Explicit read/write URI grants for camera capture, following Android's recommendation to avoid reliance on implicit grants.
- Search materializes at most 500 matching message results rather than copying all matching messages and their attachment objects into memory. New queries cancel delivery of stale results.
- Selection uses a stable read-only native editor outside recycled message rows, with soft keyboard disabled and private-window security inherited.
- User-selected visual presets share the existing theme renderer and remain individually customizable. They do not claim OEM APIs or system animation engines.

Review references:

- https://developer.android.com/about/versions/17/behavior-changes-all (memory limits, OTP handling, URI grant guidance, IME changes).
- https://news.samsung.com/global/samsung-launches-one-ui-9-beta-for-galaxy-s26-series-users
- https://www.oppo.com/en/newsroom/press/oppo-color-os-17/

The app retains its existing Android default-SMS role flow. OTP delivery and SMS/MMS carrier behavior on Android 17 still need device validation. No additional access to protected OTP messages is claimed.
