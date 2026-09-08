# Signing for Google Play

The debug-signed `san-offline-test.apk` is installable on Android 10 and newer.
**It is for testing, not Play submission.** The supplied `san-offline-unsigned.aab`
is the release bundle, but it must be signed with your upload key first.

Do not put private keys/passwords in Git, issues, screenshots, or chat. Keep an
offline backup. If this app already exists on Play, use its existing upload key
and application ID, and increase the version code. The current application ID is
`net.san.gtamod.offline`; confirm this before the first publication because it
cannot be changed for an existing Play listing.

## Build signed releases on your own machine

Install JDK 17 and Android SDK platform 36. Generate an upload key **only for a
new app** if you do not already have one; keytool prompts locally for passwords:

```sh
keytool -genkeypair -keystore "$HOME/san-upload.jks" -alias san-upload \
  -keyalg RSA -keysize 3072 -validity 10000
```

Then supply the signing configuration through environment variables, using
hidden prompts rather than writing passwords into shell history:

```bash
export ANDROID_KEYSTORE="$HOME/san-upload.jks"
export ANDROID_KEY_ALIAS="san-upload"
read -rsp 'Keystore password: ' ANDROID_STORE_PASSWORD; echo
export ANDROID_STORE_PASSWORD
read -rsp 'Key password: ' ANDROID_KEY_PASSWORD; echo
export ANDROID_KEY_PASSWORD
./gradlew :app:assembleRelease :app:bundleRelease
unset ANDROID_STORE_PASSWORD ANDROID_KEY_PASSWORD
```

Outputs:

- `app/build/outputs/apk/release/app-release.apk`
- `app/build/outputs/bundle/release/app-release.aab`

Verify with Android SDK `apksigner verify --verbose` for the APK and
`jarsigner -verify -verbose` for the AAB. Self-signed certificate trust warnings
from jarsigner are normal for Android upload keys; missing/broken signatures
are not. Enroll in Play App Signing in Play Console as appropriate. Never use
the public Android debug key for a Play release.

Alternatively, sign the already-produced AAB on a machine with JDK installed:

```bash
cp san-offline-unsigned.aab san-offline-upload.aab
jarsigner -keystore "$HOME/san-upload.jks" san-offline-upload.aab san-upload
jarsigner -verify san-offline-upload.aab
```

The APK and AAB should be signed with your intended certificates before external
distribution. To update an installed debug test APK with a release-signed APK,
uninstall the debug version first (this clears local saves), or use a separate
application ID for testing in your release process.

## Publishing checklist

- Confirm licensed rights for both the original Saints Row material and the GTA
  branding/artwork in this supplied mod. Keep licensing records available.
- Confirm the application ID, version code, title, icon and non-misleading listing.
- Sign using the private upload key and satisfy Play Console's current SDK checks.
- Test release builds on physical Android 10+ devices with different screen ratios,
  display cutouts, navigation modes and audio hardware; play through all missions.
- Complete content rating and any violence-related declarations accurately.
- Complete Data safety, privacy policy, store screenshots and account-specific
  testing requirements. The app currently has no telemetry, accounts or ads.

An emulator smoke test and a valid AAB do not guarantee Google Play approval.
