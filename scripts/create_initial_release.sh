#!/usr/bin/env bash
# One-time new-app signing. Only the PUBLIC encryption certificate is in Git.
set -euo pipefail
umask 077
if [[ -f deliverables/upload-key-backup.cms ]]; then
  echo 'A signing key already exists. Existing signed release is retained; future releases must reuse the private upload key.'
  exit 0
fi
[[ "${GITHUB_REF:-}" == 'refs/heads/arena/01a0814e-san' ]] || { echo 'Run this bootstrap only on the session branch in CI'; exit 1; }
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
export ANDROID_KEYSTORE="$WORK/san-upload.p12"
export ANDROID_KEY_ALIAS=san-upload
export ANDROID_STORE_PASSWORD=$(openssl rand -hex 32)
export ANDROID_KEY_PASSWORD="$ANDROID_STORE_PASSWORD"
printf '::add-mask::%s\n' "$ANDROID_STORE_PASSWORD"
keytool -genkeypair -noprompt -storetype PKCS12 -keystore "$ANDROID_KEYSTORE" \
  -storepass:env ANDROID_STORE_PASSWORD -keypass:env ANDROID_KEY_PASSWORD \
  -alias "$ANDROID_KEY_ALIAS" -keyalg RSA -keysize 3072 -validity 10000 \
  -dname 'CN=SAN Android Upload, O=SAN'
keytool -exportcert -rfc -keystore "$ANDROID_KEYSTORE" -storepass:env ANDROID_STORE_PASSWORD \
  -alias "$ANDROID_KEY_ALIAS" -file deliverables/upload-certificate.pem
export SIGNING_EXPORT="$WORK/private-signing-backup.zip"
python3 - <<'PY'
import os,zipfile
with zipfile.ZipFile(os.environ['SIGNING_EXPORT'],'w',zipfile.ZIP_DEFLATED) as z:
    z.write(os.environ['ANDROID_KEYSTORE'],'san-upload.p12')
    z.writestr('PASSWORD.txt',os.environ['ANDROID_STORE_PASSWORD']+'\n')
    z.writestr('README.txt', '''PRIVATE ANDROID UPLOAD KEY — DO NOT PUBLISH OR COMMIT
Application ID: net.san.gtamod.offline
Keystore: san-upload.p12 (PKCS12, RSA 3072)
Alias: san-upload
Keystore and key password: see PASSWORD.txt

Keep this entire archive in a secure password manager or encrypted offline backup.
Use this key for future uploads to the same Play listing. Enroll in Play App Signing.
The APK/AAB and public certificate can be distributed. This archive CANNOT.
See docs/SIGNING.md for build instructions. Never upload this ZIP to the public repository.
''')
PY
# Authenticated public-key envelope; its recipient private key exists ONLY in the Arena workspace.
openssl cms -encrypt -binary -aes-256-gcm -in "$SIGNING_EXPORT" -outform DER \
  -out "$WORK/upload-key-backup.cms" .github/signing-export-public.pem
./gradlew :app:assembleRelease :app:bundleRelease --stacktrace 2>&1 | tee deliverables/release-build-log.txt
cp app/build/outputs/apk/release/app-release.apk deliverables/san-offline.apk
cp app/build/outputs/bundle/release/app-release.aab deliverables/san-offline.aab
"$ANDROID_HOME/build-tools/36.0.0/apksigner" verify --verbose --print-certs deliverables/san-offline.apk > deliverables/apk-signature.txt
jarsigner -verify deliverables/san-offline.aab > deliverables/aab-signature.txt
python3 scripts/verify_package.py deliverables/san-offline.apk > deliverables/release-apk-verification.json
python3 scripts/verify_package.py deliverables/san-offline.aab > deliverables/release-aab-verification.json
cp "$WORK/upload-key-backup.cms" deliverables/upload-key-backup.cms
curl --retry 3 -fL https://github.com/google/bundletool/releases/download/1.18.3/bundletool-all-1.18.3.jar -o "$WORK/bundletool.jar"
java -jar "$WORK/bundletool.jar" validate --bundle=deliverables/san-offline.aab > deliverables/bundle-validation.txt
java -jar "$WORK/bundletool.jar" dump manifest --bundle=deliverables/san-offline.aab > deliverables/release-manifest.xml
python3 - <<'PY'
import xml.etree.ElementTree as ET
r=ET.parse('deliverables/release-manifest.xml').getroot();a='{http://schemas.android.com/apk/res/android}'
sdk=r.find('uses-sdk')
assert sdk.get(a+'minSdkVersion')=='29'
assert sdk.get(a+'targetSdkVersion')=='36'
assert sdk.get(a+'maxSdkVersion') is None
assert r.find('application').get(a+'debuggable','false')=='false'
assert {x.get(a+'name') for x in r.findall('uses-permission')} <= {'android.permission.VIBRATE'}
print('Release manifest verified: Android 10+, target 36, no max SDK, not debuggable, no network permission.')
PY
cp "$WORK/upload-key-backup.cms" deliverables/upload-key-backup.cms
printf 'Source commit: %s\nBuild run: %s/%s/actions/runs/%s\nApplication ID: net.san.gtamod.offline\nVersion: 1.0.0 (1)\nMinimum: Android 10 (API 29)\nTarget: API 36\n' \
  "$GITHUB_SHA" "$GITHUB_SERVER_URL" "$GITHUB_REPOSITORY" "$GITHUB_RUN_ID" > deliverables/RELEASE.txt
