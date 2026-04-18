# Changelog

All notable changes to PrefabOptimizer-Extended will be documented in this file.

This project currently follows the version from `manifest.json` and `build.gradle.kts`.

## [0.2.0-SNAPSHOT] - Unreleased — Extended fork

### Fork

This version forks the original [PrefabOptimizer by Theobosse / ninesliced](https://ninesliced.com) and renames the project to **PrefabOptimizer-Extended**. The AGPL-3.0 license is unchanged and all upstream credit is preserved.

### Fixed

- **Fluid-adjacent blocks are no longer silently removed.** Upstream 0.1.0 could remove blocks that sit directly next to water or lava voxels, because the six-neighbor check only looked at other blocks and ignored the fluid layer. The result was water/lava appearing to "leak" or vanish from optimized prefabs. A new setting **"Preserve blocks next to water / lava"** (enabled by default) pre-scans fluid positions and refuses to remove any block whose 6-neighbor position contains a fluid. The same check applies to the selection optimizer via a fluid-aware `OccludedBlockMask`.

### Changed

- Rebranded artifact: `dev.ninesliced:PrefabOptimizer` → `dev.lewai:PrefabOptimizer-Extended`.
- Moved Java package: `dev.ninesliced.prefaboptimizer.*` → `dev.lewai.prefaboptimizerextended.*` to avoid classloader collisions with the upstream mod.
- Migrated the build system from Maven (`pom.xml`) to Gradle (`build.gradle.kts` + wrapper).
- Player-facing chat prefixes and log lines now use `PrefabOptimizer-Extended` instead of `PrefabOptimizer` so the two mods are distinguishable in logs.
- Manifest `Description` and `Authors` now credit Theobosse as original author and lewai1 as fork maintainer.

### Planned in 0.2.0

- Live progress bar with per-prefab progress updates and a cancel button.
- O(1) neighbor lookups inside prefab batch optimization (HashMap indexing).
- Cached block classification per optimization run.
- Parallel prefab batch processing across CPU cores.
- Warn on invalid exclusion regex instead of silently falling back to token matching.
- Atomic prefab writes (temp + rename on success).
- Dry-run / preview mode for destructive batches.
- New optimization modes: aggressive cube, flood-fill interior, shell thickness.

## [0.1.0] - 2026-04-15 — Upstream release by Theobosse

Initial release of PrefabOptimizer by [Theobosse / ninesliced](https://ninesliced.com).

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

### Known Limitations (at 0.1.0)

- Only the conservative hidden full-cube optimization mode exists in this release.
- Batch prefab optimization writes prefab files and is not integrated with `/undo`.
- Aggressive optimization modes are not implemented yet.
- A dedicated block picker for exclusions is not implemented yet.
- Progress reporting is status/message based rather than a detailed progress bar.
- There are no automated tests yet.
