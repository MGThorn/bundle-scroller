# Bundle Scroller

A client-side Fabric mod for Minecraft 1.21.11 that makes bundles work with drag gestures, the way [Item Scroller](https://github.com/sakura-ryoko/itemscroller) does for normal items.

## Features

With a bundle on the cursor:

| Input | Effect |
|---|---|
| Hold left and drag | Every filled slot you pass over is pulled into the bundle. Empty slots are skipped |
| Release left over an empty slot | The bundle is put down in that slot |
| Hold right and drag | One item from the bundle goes into every empty slot you pass over. Filled slots are skipped |

- Each slot is handled once per drag.
- Single clicks still work as in vanilla.
- Every step is a normal vanilla click, so the mod works on servers without anything installed there.

## Configuration

- **A + C** opens the main menu, which leads to the config screen.
- `bundleDrag` (Generic) turns bundle dragging on or off.
- `openGuiConfigs` (Hotkeys) can be bound to open the config screen directly.

The config is saved in `config/bundle-scroller.json`.

## Requirements

- Fabric Loader 0.19.5 or newer
- Fabric API
- [MaLiLib](https://github.com/sakura-ryoko/malilib) 0.27.20 or newer
- [Item Scroller](https://github.com/sakura-ryoko/itemscroller) 0.30.8 or newer

## Building

```
./gradlew build
```

The jar is written to `build/libs/`.

## License

[LGPL-3.0-only](LICENSE)
