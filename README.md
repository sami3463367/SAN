# SAN · Offline Android port

An Android **10 and higher** adaptation of the exact J2ME archive supplied by the
repository owner. It runs the original game logic and bundles the original
assets; it is not a replacement game drawn from screenshots.

> The archive is named “GTA5 Mobile,” but its executable and story are the
> **Saints Row 2 mobile game with GTA-branded modifications**. This project does
> not contain the desktop/console GTA V game.

## Signed downloads · version 1.0.0

- **[Signed APK](deliverables/san-offline.apk)** — install on an Android 10+ phone.
- **[Signed AAB](deliverables/san-offline.aab)** — upload bundle for the new Play listing.
- [Checksums](deliverables/SHA256SUMS) · [Release provenance](deliverables/RELEASE.txt)
- [Verification results](docs/VERIFICATION.md) · [Signing and key-backup instructions](docs/SIGNING.md)

The private upload-key backup, `SAN-private-signing-backup.zip`, is provided
separately in the Arena workspace and **is not in GitHub**. Download it and store
it securely. Never publish that ZIP. The repository contains only the public
certificate and an encrypted recovery envelope.

The signed AAB is not the older `san-offline-unsigned.aab`. The older
`san-offline-test.apk` is debug-signed and is not the release APK. Google Play
listing, content rating, licensing and physical-device QA still need completing.
The checked-in signed files are the release snapshot identified in `RELEASE.txt`;
future builds must reuse your saved private upload key, not generate a new one.

## Verification

The completed [build run](https://github.com/sami3463367/SAN/actions/runs/34242193137)
passed **7 JVM tests** plus the **2 instrumentation tests on each of Android 10
and Android 16**. The instrumentation suite covers original-game startup,
character creation, gameplay, simultaneous native touch inputs, independent
finger release/cancellation, save writing, local audio players, background pause,
launcher resume, sprite transforms and clipping.

Release APK signature verification and bundletool AAB validation passed. The
private backup's certificate was matched to the release certificate. These are
emulator checks, not a full mission playthrough or a guarantee of Play approval.

## Android support

| Setting | Value |
|---|---|
| Minimum | **Android 10 / API 29** |
| Target / compile SDK | **API 36** |
| Maximum | **None set** |
| Orientation on phones | Sensor landscape (either horizontal rotation) |
| Rendering | Original 240 × 320 viewport, proportional nearest-neighbor scaling |
| Input | Native multitouch; optional keyboard/gamepad button mappings |
| Runtime connectivity | Completely offline; no INTERNET permission |
| Native dependencies | None; no Unity, WebView or external emulator required |
| Storage | On-device private record store; cloud backup disabled |

The game image is not stretched or cropped. Touch controls use the space beside
it. Its hard-coded portrait camera cannot become full-width landscape without
changing the original layout and logic.

## Controls

- **Left pad:** move/steer/navigate. Hold a direction while pressing fire.
- **FIRE / OK:** attack or confirm.
- **1 / VEHICLE:** enter/leave a car.
- **\* / WEAPON:** switch weapon. **# / TARGET:** switch target.
- **SELECT:** original left soft key/context action.
- **MENU / BACK:** original right soft key; Android back maps here too.
- **KEYS:** full phone number pad for text input and number prompts.
- **SOUND:** mute toggle. **?:** touch instructions.

Character creation uses the D-pad to choose gender, SELECT to confirm, and KEYS
for the original multi-tap name entry. The original settings include steering
and directional driving modes. Progress follows the original game's checkpoint
semantics; uninstalling the app clears its saves.

## Build

Requirements: JDK 17, Android SDK platform 36, internet for build dependencies.
No Unity installation is needed. Runtime does not need internet.

```sh
./gradlew testDebugUnitTest assembleDebug bundleRelease
python3 scripts/verify_package.py app/build/outputs/apk/debug/app-debug.apk
python3 scripts/verify_package.py app/build/outputs/bundle/release/app-release.aab
```

GitHub Actions installs the tools, builds the app and performs Android emulator
checks. All work is on `arena/01a0814e-san`.

## Inspection and fidelity

- [Detailed inspection/architecture](docs/PORTING.md)
- [Visual asset gallery](docs/asset-gallery.html) (download and open in a browser)
- [Per-file SHA-256 inventory and decoded text](docs/asset-inventory.json)
- Original bytecode inspection in `docs/inspection/`; not used as replacement source.

All **54 original non-code assets** are checked byte-for-byte in both packages.
The original 10 classes are converted to DEX by Android's build tools.

This is an initial port, not a claim of perfect emulation of every old handset.
Android MIDI synthesis and arc-edge rasterization can differ. Full mission
playthroughs, physical-device QA remain necessary before publishing. Publishing rights were confirmed by the repository owner, not
independently verified by this project.
