# Changelog

All notable changes to PrefabOptimizer will be documented in this file.

This project currently follows the version from `manifest.json` and `pom.xml`.

## [0.1.0] - 2026-04-15

Initial release of PrefabOptimizer.

### Included

- Selection optimizer GUI for BuilderTools selections.
- Prefab batch optimizer GUI for optimizing prefab files and folders.
- Main `/prefaboptimizer` command with `/popt` and `/prefabopt` aliases.
- `/prefaboptimizer selection` subcommand with `/prefaboptimizer sel` alias.
- `/prefaboptimizer prefabs` subcommand with `/prefaboptimizer prefab` and `/prefaboptimizer batch` aliases.
- BuilderTools-compatible selection optimization with `/undo` support.
- In-game prefab browser for selecting prefab sources.
- Individual prefab source selection.
- Folder source selection.
- Optional recursive folder search.
- Target writable asset-only mod selection.
- Target folder selection inside `Server/Prefabs`.
- Conservative hidden full-cube optimization mode.
- Transparent and non-opaque block preservation.
- Strict cube-only mode.
- Optional `CubeWithModel` consideration when strict cube-only mode is disabled.
- Custom block exclusions by regex.
- Custom block exclusions by comma-separated, semicolon-separated, or newline-separated tokens.
- Background prefab batch processing.
- Player-facing status and warning messages.
- Automatic GUI close when a valid optimization starts.
- Completion messages with removed block counts and optimization percentage.
- Preservation of prefab entities, fluids, tints, and block component holders.
- Preservation of relative folder structure for folder batch sources.
- Compatibility shims for current BuilderTools selection and block mask APIs.

### Technical Notes

- Targets Hytale server `2026.03.26-89796e57b`.
- Compiles with Java `25`.
- Uses Hytale's provided `com.hypixel.hytale:Server` dependency.
- Batch optimization writes optimized prefab copies through `PrefabStore.savePrefabToPack(...)`.
- Selection optimization runs through BuilderTools history using a custom `OccludedBlockMask`.

### Known Limitations

- Only the conservative hidden full-cube optimization mode exists in this release.
- Batch prefab optimization writes prefab files and is not integrated with `/undo`.
- Aggressive optimization modes are not implemented yet.
- A dedicated block picker for exclusions is not implemented yet.
- Progress reporting is status/message based rather than a detailed progress bar.
- There are no automated tests yet.
