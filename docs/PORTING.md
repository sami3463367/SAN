# Original game inspection and Android integration

## Source and identity

The user uploaded `GTA 5 MOD [240x320] (andrew-lviv.net).jar.zip` to `main`.
It was retrieved with Git and imported onto the session branch. The file is
already a valid JAR-format ZIP, not a ZIP containing another JAR. The byte-identical
copy used by Android is `app/libs/original-game.jar`.

SHA-256: `2ef3bba798e5a673428212d35cabdec0d7658c4013471d01c3933ad7408d4735`

Manifest: `GTA5 Mobile`, version `1.0.7`, entry point `SaintsRow2`, MIDP 1.0,
CLDC 1.0. Despite the manifest, rendering actually calls MIDP 2.0 `drawRegion`.
The graphics include GTA V branding, but the executable, maps, story, original
credits and characters are Saints Row 2 mobile content. Credits identify THQ
and G5 Entertainment (2008). This is not a port of desktop/console GTA V.

## Inventory

- 10 obfuscated Java classes: `SaintsRow2`, `a` through `i`.
- 65 file entries, including the JAR manifest.
- Original PNGs include characters, vehicles, buildings, HUD, fonts, logos,
  mission previews, portraits, effects, shadows and backgrounds.
- Six `.lvl` files: `Street`, `Brotherhood`, `Ronin`, `SonsOfSamedi`, `Ultor`,
  and the small `Creep` level/resource. Five are substantial world-region files.
- `bin.all` contains 20 palettes, font metrics, five localized string tables,
  sprite/animation definitions, sound metadata, items, missions and other data.
- Five languages: English, French, Italian, German, Spanish.
- MIDI: `MainMenu.mid`, `use.mid`.
- Six AMR effects: AK-47, explosion, melee, pistol, shotgun, Uzi (filenames are preserved rather than renamed).

`asset-inventory.json` contains per-entry sizes and SHA-256 hashes, original
image dimensions, and the decoded localized strings. `asset-gallery.html` is a
self-contained visual inventory. `inspection/` contains CFR output and JVM
bytecode disassembly for investigation, **not source compiled into the app**.
Decompiler output can be imperfect. The original `.class` files are used instead.

## Execution architecture

Gradle/D8 converts the original JAR classes into the application's DEX at build
time. Original Java resources are packaged at APK class-loader resource paths.
There is no external emulator app, file picker, dynamic code download, Unity,
WebView, network service, or runtime installation step.

A narrowly scoped, original Android implementation supplies the MIDP APIs used
by this archive:

| Original interface | Android implementation |
|---|---|
| MIDlet and Display | One original game session per Android process |
| Canvas | 240 × 320 bitmap, original game-thread rendering |
| Graphics | Pixel-grid drawing, replaceable clipping, all 8 sprite transforms |
| Image | Original PNG decoding, density scaling disabled |
| Key events | Native MotionEvent pointer IDs, key reference counts, frame-boundary queue |
| RMS | Private local files, original save bytes, atomic replacement and recovery backup |
| JSR-135 Player | Local MediaPlayer MIDI/AMR playback, state/volume/event adaptation |
| platformRequest | Offline no-op; never opens external URLs |

Android 10 is API 29. The project compiles and targets API 36. There are no native
`.so` libraries, avoiding native ABI and 16 KB ELF page-alignment dependencies.
This does not by itself guarantee every Play Console acceptance check.

## What is preserved and what changes

**Preserved:** original mission logic, level data, sprites, palette swapping,
text/bitmap fonts, story, menus, sound files, save serializer and gameplay code.
A package verifier compares every original non-code asset byte-for-byte against
both the APK and AAB.

**Adapted:** the MIDP platform implementation, Android lifecycle, local storage,
and native multitouch controller. The original code hard-codes 240 × 320 in
many places, including viewport construction and menus. To avoid distortion,
artwork is aspect-fit at its native ratio with control panels occupying the sides.
It is deliberately not stretched to full landscape or cropped. Filling the whole
landscape screen with gameplay would require a separate camera/UI rewrite.

**Fidelity limits:** Android MIDI synthesizers may sound different from an old
handset. Android rasterization of arcs can differ at individual boundary pixels.
The source has pre-existing mixed branding/art quality. No claim is made that
all missions have been played through or that output is pixel-perfect against a
reference handset. Optional original vibration is not currently emulated.

## Controls verified from the original help text

- D-pad: move, navigate, steer. Negative directional key codes ensure navigation
  works even when the original numeric keys are in text-entry mode.
- Fire/OK (center): attack/confirm.
- `1`: enter or leave a vehicle.
- `*`: switch weapon.
- `#`: switch target.
- Left soft key: context action/select/talk/rob.
- Right soft key: pause/menu/back.
- Full number pad: original phone-style text entry and numerical prompts.
- Multiple pointers can hold movement and fire at the same time. Releasing a
  second pointer sharing a key does not release the first pointer's key.
- Touch cancellation, app backgrounding and focus loss release held controls.

The built-in game settings still offer steering/directional driving modes.

## Offline and privacy

No INTERNET, network state, SMS, billing, advertising ID, analytics or external
storage permissions. Resources and audio are bundled. Record stores use internal
private storage. Audio extraction uses private cache. No accounts or telemetry.
Android cloud backup is disabled. Uninstalling/clearing app data removes saves.
This is fully offline **at runtime**; building the app requires SDK/Gradle downloads.

## Rights and release boundaries

The repository owner confirmed publishing rights. This is not independent legal
verification. Keep permission covering both the underlying Saints Row content
and GTA-branded material; repository access alone would not provide that right.
Do not submit a build under misleading claims about being the official GTA V.

Test APKs use debug signing. The provided unsigned AAB requires the owner's
private upload key before Play submission. No signing secrets are committed.
See `SIGNING.md`. Store listing, content rating, privacy disclosures and any
app/account-specific testing requirements remain Play Console tasks.
