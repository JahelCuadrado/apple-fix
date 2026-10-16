# AppleFix

Minecraft on a Mac, without the little things that go wrong.

Fabric mod for Minecraft 26.3. Client only. **Beta.**

## What it fixes
- **Trackpad scrolling:** a swipe no longer races across the whole hotbar; it moves a few slots,
  with an adjustable speed. Momentum stops when you lift your fingers (or keeps gliding, if you
  prefer).
- **Smooth menus:** lists and inventories scroll with momentum again.
- **Command + Q drops a whole stack,** like Control + Q (when the quit shortcut is off).
- **Saving and opening files no longer crashes the game:** mods with a file picker (like
  Scribble's save and load buttons for books) work on macOS.
- **Quitting the game is instant:** no more 15-second hang or false crash report when a mod leaves
  background tasks running.
- **Leaving a world with Distant Horizons no longer freezes the game** for several seconds.
- **On every system:** reverse scrolling and a scroll lock while attacking or using an item.

## Settings
Options → Controls → **AppleFix…** (also from Mod Menu, if installed).

## Compatibility
- Works without Fabric API. Mod Menu is optional.
- Replaces *MacOS Input Fixes*: do not install both.
- Outside macOS it only adds the options that apply to every system.

## Development

Requires JDK 25. `./gradlew runClient` · `./gradlew build` (compile, format check, JUnit)
· `./gradlew spotlessApply`.

## License

MIT.
