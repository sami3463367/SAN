# SAN · Offline Android port

An Android **10 and higher** adaptation of the exact J2ME archive supplied by the
repository owner. It runs the original game logic and bundles the original
assets; it is not a replacement game drawn from screenshots.

> The archive is named “GTA5 Mobile,” but its executable and story are the
> **Saints Row 2 mobile game with GTA-branded modifications**. This project does
> not contain the desktop/console GTA V game.

## Downloads

- [`deliverables/san-offline-test.apk`](deliverables/san-offline-test.apk) —
  installable, **debug-signed test build** for Android 10+.
- [`deliverables/san-offline-unsigned.aab`](deliverables/san-offline-unsigned.aab) —
  release bundle, **requires your upload-key signature before Play submission**.
- [`deliverables/SHA256SUMS`](deliverables/SHA256SUMS) — checksums.

These are actual Android build outputs. See the test evidence in `deliverables/`
and [signing instructions](docs/SIGNING.md). No private signing key is committed.

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
playthroughs, physical-device QA and release signing remain necessary before
publishing. Publishing rights were confirmed by the repository owner, not
independently verified by this project.
