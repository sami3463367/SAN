# Android downloads

- `SAN-Android-Download.zip`: the signed APK and AAB together, plus an important notice.
- `san-offline.apk`: signed Android 10+ app.
- `san-offline.aab`: signed Android App Bundle.

The ZIP does **not** contain the private signing-key backup. That backup and its
private recovery key are unavailable in the current workspace. The encrypted
recovery envelope alone cannot recover the key. If you did not save the original
private backup, resolve key recovery or replacement before publishing so that
future uploads can be signed.

These files are hosted on the session branch. No pull request or merge was made
as part of making the downloads available.
