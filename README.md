# Drill Down

Drill Down is a Java/libGDX factory game codebase focused on PC play and LAN multiplayer.

## Current Scope
- PC-only runtime
- LAN host/client multiplayer
- Host-authoritative sync model

## Project Layout
- `core/` - game logic, scenes, UI, structures, and network code.
- `desktop/` - desktop launcher and desktop-specific build/runtime code.
- `commons/` - shared utility and annotation modules.
- `gdx-sfx/` - shared audio support.
- `assets/` - shared game resources.

## Main Entry Points
- Desktop launcher: `de.dakror.quarry.desktop.DesktopLauncher`
- LAN host: `de.dakror.quarry.scenes.Game.startLanHost(...)`
- LAN join: `de.dakror.quarry.scenes.Game.joinLanClient(...)`

## Build
Use Gradle from the repository root:

- `./gradlew desktop:run` for a local desktop run
- `./gradlew desktop:dist` for a distributable desktop build

## Notes
- The codebase uses a host-authoritative LAN flow with snapshot join and command sync.
- Android-specific entry points and mobile resource paths have been removed from the active PC setup.
