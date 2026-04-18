# Changelog

All notable changes to PrefabOptimizer-Extended will be documented in this file.

This project currently follows the version from `manifest.json` and `build.gradle.kts`.

## [0.2.0-SNAPSHOT] - Unreleased — Extended fork

### Fork

This version forks the original [PrefabOptimizer by Theobosse / ninesliced](https://ninesliced.com) and renames the project to **PrefabOptimizer-Extended**. The AGPL-3.0 license is unchanged and all upstream credit is preserved.

### Fixed

- **Fluid-adjacent blocks are no longer silently removed.** Upstream 0.1.0 could remove blocks that sit directly next to water or lava voxels, because the six-neighbor check only looked at other blocks and ignored the fluid layer. The result was water/lava appearing to "leak" or vanish from optimized prefabs. A new setting **"Preserve blocks next to water / lava"** (enabled by default) pre-scans fluid positions and refuses to remove any block whose 6-neighbor position contains a fluid. The same check applies to the selection optimizer via a fluid-aware `OccludedBlockMask`.

### Performance

- **O(1) neighbor lookups in prefab batch optimization.** `PrefabBatchOptimizer` now pre-builds a `Long2IntOpenHashMap` of block ids keyed by packed local position and a `LongOpenHashSet` of positions with non-zero filler, so neighbor checks no longer go through `getBlockHolderAtWorldPos` (which re-acquires the selection's read lock six times per block).
- **Block classification cache.** Both `PrefabBatchOptimizer` and `OccludedBlockMask` now memoize `BlockClassifier.isOptimizableFullCube(blockId, settings)` per optimization run in an `Int2BooleanOpenHashMap`. Each block id is classified once instead of up to seven times (self + six neighbors).
- **Parallel prefab batch processing.** `PrefabOptimizerService` replaces the single-threaded batch executor with a `ForkJoinPool` sized to `max(2, cores − 1)`. `PrefabBatchOptimizer.optimize` now iterates prefabs via `parallelStream()` inside that pool, with thread-safe accumulators (`AtomicInteger`, `Collections.synchronizedList`) for counters and warnings. Saving to different prefab paths is independent and relies on `PrefabStore.getPrefab`'s existing `ConcurrentMap.computeIfAbsent` cache.

### UX

- **Invalid exclusion regex now surfaces a warning** instead of silently falling back to token matching. `OptimizerSettings` captures the `PatternSyntaxException` description, the batch optimizer adds it to the result's warning list, and the selection optimizer sends it as a chat message before queueing the BuilderTools operation.

### Added

- **Flood-fill interior optimization mode (opt-in).** New "Flood-fill interior mode" checkbox in both the Selection and Prefab Batch Optimizer UIs, disabled by default. When enabled, a 3D BFS seeded from the exterior of the bounding box propagates through air, transparent blocks, and fluids; any optimizable block whose six neighbor positions are all unreachable from the exterior is considered interior and removed. This catches thick walls with sloped inner surfaces, dense solid masses, and other geometries where the default six-neighbor rule left a one-block-thick inner shell. The `Preserve blocks next to water / lava` and `Exclude blocks` safeguards continue to apply in flood-fill mode. Prefab path uses the pre-built block index; selection path uses a separate `SelectionFloodFill` helper driven by the `ChunkAccessor`.
- **Shell thickness N (flood-fill companion setting).** New numeric field "Shell thickness" in both UIs. When flood-fill is on, the BFS result is expanded by `N - 1` additional BFS steps through solid blocks, so `N` visible layers are preserved. `N = 1` reproduces the original flood-fill behavior; `N = 3` keeps a three-block-thick skin on every visible face. Clamped to `[1, 64]`.
- **Skip bottom face.** New checkbox in both UIs. When on, the prefab's bottom face is treated as "resting on the ground": the six-neighbor check assumes the `-Y` neighbor is solid, and flood-fill no longer seeds exterior air below `yMin`. Useful for foundation-heavy prefabs where the bottom is never visible at runtime.
- **Preview-only mode.** New checkbox. Runs the full optimization pipeline but skips `savePrefabToPack` (batch) or `state.replace` (selection). The status message reports "would remove X/Y blocks" so builders can validate their settings before a destructive batch.
- **Live progress + cancel for batch optimization.** The Prefab Batch Optimizer page no longer auto-closes on submit. Instead it stays open and streams progress updates as each prefab completes (`Optimizing 743/2000 (37%)`). The primary button swaps from `Optimize Prefab Batch` to `Cancel Batch` while running; clicking cancel flips an `AtomicBoolean` that workers check between prefabs, short-circuiting the remainder.
- **Atomic prefab writes.** `PrefabBatchOptimizer` saves each optimized prefab through a `*.tmp.<nanos>` key first, then `Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)` to the final path (with a fallback to `REPLACE_EXISTING` only when the filesystem doesn't support atomic move). On write failure the temp file is deleted and the old file stays intact.
- **Automatic batch backups + `/prefaboptimizer undo` command.** Before overwriting an existing prefab, the batch copies the current file into `<pack>/Server/Prefabs/__PrefabOptimizerBackup__/<timestamp>/<key>`. The new `/prefaboptimizer undo <pack>` subcommand restores the most recent batch backup, listing how many prefabs were restored in chat.
- **Exclusion preset buttons.** `+ Fluids` (`water,lava`), `+ Decor` (`leaves,vines,flowers,grass,torch,door,window,sign`), and `Clear` buttons below the exclusion text field in both pages. One-click for the most common cases.
- **Inline block browser (Prefab Batch only).** A scrollable filter panel under the exclusion field lists live `BlockType.getAssetMap()` entries matching a search query (up to 30). Clicking a suggestion appends the exact block id to the exclusion field.
- **JMH benchmark harness.** `src/jmh/` with benchmarks for `PackedPos.pack/unpack` and `ReachabilityMap.add/contains`. Run with `./gradlew jmh`.

### Performance (continued)

- **BitSet-backed flood-fill reachability.** `ReachabilityMap` uses a bounds-relative `BitSet` (O(V) bits) instead of a `LongOpenHashSet`. On a 256³ prefab this cuts reachability storage from ~500 MB to ~2 MB, making flood-fill tractable on very large prefabs.
- **Cached reflection in `SelectionBoundsReader`.** `getSelectionMin` / `getSelectionMax` `Method` handles are resolved once in a static initializer. Vector component field/method lookups are cached per vector class in a `ConcurrentHashMap<Class<?>, VectorAccessor>`.

### Testing

- **JUnit 5 test suite.** `src/test/` covers `OptimizerSettings` defaults, exclusion parsing (tokens + regex + invalid regex), `shellThickness` parsing, mode flag independence, `ReachabilityMap.add/contains/forEachPosition`, `BlockBounds` inclusive bounds, and `PackedPos.pack/unpack` sign-extension. 24 tests total.

### Build

- **Gradle configuration cache enabled.** `gradle.properties` sets `org.gradle.configuration-cache=true`, `org.gradle.caching=true`, and `org.gradle.parallel=true`. Subsequent builds reuse the task graph (`Reusing configuration cache.`) for a noticeable speedup.

### Changed

- Rebranded artifact: `dev.ninesliced:PrefabOptimizer` → `dev.lewai:PrefabOptimizer-Extended`.
- Moved Java package: `dev.ninesliced.prefaboptimizer.*` → `dev.lewai.prefaboptimizerextended.*` to avoid classloader collisions with the upstream mod.
- Migrated the build system from Maven (`pom.xml`) to Gradle (`build.gradle.kts` + wrapper).
- Player-facing chat prefixes and log lines now use `PrefabOptimizer-Extended` instead of `PrefabOptimizer` so the two mods are distinguishable in logs.
- Manifest `Description` and `Authors` now credit Theobosse as original author and lewai1 as fork maintainer.

### Still planned for a later release

- Full-page block picker with persistent checkbox state (current implementation is an inline search+append panel).
- Aggressive cube mode (allow more `DrawType` values as occluders).
- Export optimization report to CSV.
- Incremental batch (skip prefabs already optimized).
- Optimization stats embedded in output prefab metadata.

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
