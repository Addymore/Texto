# Texto APK signing

The GitHub release APK uses `app.texto.sms` with the Texto release certificate first created for 1.6.0. The debug APK uses `app.texto.sms.debug` and retains the development certificate so existing development installations can update.

Keep the release signing key and its password in a secure, backed-up location. They are intentionally excluded from Git and from source archives. Losing this key prevents updates signed with the same certificate. Do not upload it as a release asset.

The normal Gradle release task remains unsigned for F-Droid. GitHub release packaging signs its output separately with Android SDK apksigner, then checks signature and alignment. Future GitHub APK releases must reuse the same release key.

F-Droid signs `app.texto.sms` with its own key. Despite the same package name, F-Droid and GitHub release APKs cannot update one another unless their signing arrangements are explicitly coordinated. The debug package is separate from both. App-private PINs, privacy rules, themes and recycle-bin markers do not migrate between packages. Configure protection before switching the default SMS application.
