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

### Picking from bundles

- **Pick list:** in the A + C menu, open "Pick list" and click items to add them. The list order is the cycle order.
- **`pickFromBundle` hotkey** (unbound by default):
  - With an empty main hand, it takes the next pick list item out of a bundle into your hand.
  - With a pick list item in your hand, it puts that item back and takes the next one out, so each press steps through the list. Other items are left alone.
- **`putIntoBundle` hotkey** (unbound by default): puts the item in your hand into a bundle (any item). Whatever doesn't fit stays in your hand.
- **`autoRestockFromBundle`** (off by default): when the stack in your hand runs out, the same item is refilled from a bundle. `autoRestockAfterDrop` decides whether dropping the last item with Q also refills it.

### Item Scroller crafting from bundles

Item Scroller's crafting (recipe clicks, scroll crafting, craft everything and mass craft) takes missing ingredients out of bundles into free slots first (`bundleCrafting`).

### Auto eat from bundle (needs Garitone Mineman)

When Garitone Mineman's easy eat or auto eat finds no food in your inventory, Bundle Scroller takes food out of a bundle into a free slot, and easy eat eats it. Mineman's `autoEatBlacklist` also applies to the food in bundles. Without Garitone Mineman the `autoEatFromBundle` option does nothing, and its tooltip shows a red warning.

## Configuration

- **A + C** opens the main menu, which leads to the config screen.
- `bundleDrag` (Generic) turns bundle dragging on or off.
- `autoEatFromBundle` (Generic) turns auto eat from bundle on or off.
- `openGuiConfigs` (Hotkeys) can be bound to open the config screen directly.

The config is saved in `config/bundle-scroller.json`.

## Requirements

- Fabric Loader 0.19.5 or newer
- Fabric API
- [MaLiLib](https://github.com/sakura-ryoko/malilib) 0.27.20 or newer
- [Item Scroller](https://github.com/sakura-ryoko/itemscroller) 0.30.8 or newer
- Optional: [Garitone Mineman](https://github.com/MGThorn/garitone-mineman), for auto eat from bundle

## Building

```
./gradlew build
```

The jar is written to `build/libs/`.

## License

[LGPL-3.0-only](LICENSE)
