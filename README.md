# Calculator

Sci-fi HUD styled calculator for Android (phone and tablet): scientific, boolean algebra, programmer and matrix modes.

## Build
Push to GitHub -> Actions -> "Build Debug APK" -> download artifact `Calculator-debug-apk`.

## Releasing an update
1. Change `appVersionName` in `app/build.gradle.kts` (and `version` in `metadata.json`).
2. Push, wait for the build, download the APK.
3. GitHub -> Releases -> new release, tag `v1.0.1` (same number), attach the APK, publish.

The app checks `releases/latest` of `mahfuz0712/Calculator` on launch and from INFO -> CHECK FOR UPDATES.
If the tag is newer than the installed version it shows the update dialog and opens the APK download.
`app/debug.keystore` keeps the signature identical on every build, so the new APK installs over the old one.

Author: Mohammad Mahfuz Rahman · mahfuzrahman0712@gmail.com · https://github.com/mahfuz0712
