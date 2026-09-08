# Signing for Google Play

## Delivered release

- `deliverables/san-offline.apk`: release-signed installable APK.
- `deliverables/san-offline.aab`: release-signed upload bundle.
- Application ID: `net.san.gtamod.offline`.
- Version: `1.0.0`, version code `1`.
- Keystore type: PKCS12; RSA 3072; alias: `san-upload`.
- Public certificate: `deliverables/upload-certificate.pem`.
- Certificate SHA-256:
  `23ae1a3126729c7c847304e79fb714ba23f6d26fff6b49a08eec6a9095df7ca0`.

A **new-app upload key was generated with the owner's approval**. Do not create
another key for updates to this listing. No private signing material is in Git.

## Download and protect the private backup

`SAN-private-signing-backup.zip` is in the private Arena workspace, not GitHub.
It contains:

- `san-upload.p12`: the private upload keystore.
- `PASSWORD.txt`: its generated password (also the key password).
- `README.txt`: alias, application ID and handling instructions.

**Save the entire ZIP in an encrypted offline backup or password manager. Never
upload it to the public repository, Play store listing, issues, or chat.** Do not
share the password or keystore publicly. The backup was decrypted successfully,
and its public certificate was compared with the release certificate.

The build runner encrypted this ZIP using the workspace's public transport
certificate before exporting it. `deliverables/upload-key-backup.cms` is that
AES-256-GCM encrypted recovery envelope; it is not the plaintext private key.
The recipient private transport key stays inside ignored `.signing/` in the
Arena workspace. `scripts/recover_signing_backup.py` can recover the ZIP there.
Cloning GitHub alone does **not** recover the private key, so download the backup.

## Build future signed updates on your machine

Install JDK 17 and Android SDK platform 36. Extract the private backup into a
secure directory outside the repository. Increase `versionCode` in
`app/build.gradle` for every subsequent Play upload. Keep the application ID.

Supply signing details via environment variables and a hidden local prompt:

```bash
export ANDROID_KEYSTORE="$HOME/private-san/san-upload.p12"
export ANDROID_KEY_ALIAS="san-upload"
read -rsp 'Keystore password: ' ANDROID_STORE_PASSWORD; echo
export ANDROID_STORE_PASSWORD
export ANDROID_KEY_PASSWORD="$ANDROID_STORE_PASSWORD"
./gradlew :app:assembleRelease :app:bundleRelease
unset ANDROID_STORE_PASSWORD ANDROID_KEY_PASSWORD
```

Outputs are `app/build/outputs/apk/release/app-release.apk` and
`app/build/outputs/bundle/release/app-release.aab`. The ordinary CI workflow
builds unsigned development bundles without your private key. Its one-time key
bootstrap will not replace an existing key or silently re-sign future versions
with a different certificate. The checked-in signed files remain the release
snapshot recorded in `deliverables/RELEASE.txt` until you supply a signed update.

Verify with `apksigner verify --verbose` and `jarsigner -verify`. The delivered
APK uses APK Signature Scheme v2, supported by Android 10+. The AAB passes
JarFile signature verification and bundletool validation. Current JDK jarsigner
also emits self-signed/no-timestamp warnings and JarInputStream-order warnings
because AGP places the JAR manifest near the end of the bundle; the full
verification output is preserved in `deliverables/aab-signature.txt` rather than
hidden. These checks do not guarantee Play Console approval.

Enroll in Play App Signing for the new app. Play may use an app-signing
certificate different from this upload certificate. Installing between builds
signed with different certificates can require uninstalling first, which clears
local saves. Do not use `san-offline-test.apk` (debug signed) or
`san-offline-unsigned.aab` for the production release.

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
