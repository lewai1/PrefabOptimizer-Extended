# PrefabOptimizer-Extended

> **Fork notice.** This project is an extended fork of [PrefabOptimizer](https://ninesliced.com) originally created by **Theobosse / ninesliced**. All the original optimization logic, architecture, and BuilderTools integration is their work. PrefabOptimizer-Extended adds performance, UX, and optimization-mode improvements on top, while keeping the same `/prefaboptimizer` commands and the same AGPL-3.0 license.
>
> Please send a star and thanks to the original project: <https://ninesliced.com>.

PrefabOptimizer-Extended is a Hytale builder utility mod that **removes hidden, fully enclosed blocks from BuilderTools selections and prefab files**. It turns an internal optimization script into a practical in-game tool that builders and mod creators can use without leaving Hytale.

The optimizer keeps visible surfaces, entities, fluids, tints, and block component holders intact while removing blocks that are completely surrounded by optimizable full-cube blocks.

## Goals of the Extended fork

The original mod ships a correct and conservative first mode. This fork is about making it **faster**, **nicer to use**, and **more aggressive when the builder asks for it**, without changing the conservative defaults.

- **Faster batches.** Index prefab blocks for O(1) neighbor lookups, cache block classifications, parallelize prefab batch processing across CPU cores.
- **Live progress.** Replace the fire-and-forget chat notification with a real progress bar in the GUI, per-prefab counters, and a cancel button.
- **Better exclusion UX.** Warn when a regex fails to compile instead of silently falling back to token matching.
- **Safer writes.** Atomic prefab writes (write to temp, rename on success) so a crash mid-batch never leaves half-written prefabs.
- **New optimization modes (opt-in).**
  - *Aggressive cube*: allow more block draw types as occluders.
  - *Flood-fill interior*: 3D BFS from the exterior bounding box; anything unreachable from outside is considered interior and removable. Handles hollow caverns, sloped walls, and curved surfaces where the 6-neighbor rule misses large hidden volumes.
  - *Shell thickness*: keep N layers of blocks from any exterior-visible face.
- **Dry-run preview.** Report "X blocks would be removed across Y prefabs" without writing output, so you can validate before a destructive batch.

See [`CHANGELOG.md`](./CHANGELOG.md) for what has actually shipped in each version.

## Features (current)

- Optimize the current BuilderTools selection from an in-game GUI.
- Queue selection edits through BuilderTools so `/undo` can revert the optimization.
- Batch-optimize prefab files and folders from an in-game prefab browser.
- Save optimized prefab copies into a selected writable asset-only mod.
- Preserve folder structure when optimizing folders.
- Optionally search selected folders recursively.
- Exclude transparent or non-opaque blocks by default.
- Exclude custom blocks by regex or comma-separated text tokens.
- Run prefab batch optimization in the background to avoid blocking the UI/world thread.
- Report source and per-prefab warnings back to the player instead of silently skipping failures.

## Requirements

- Hytale server build `2026.03.26-89796e57b`.
- Java `25`.
- Gradle (the wrapper is included, so you don't need a system install — you can use `./gradlew`).
- Creative-mode permission for the commands.

The mod depends on Hytale's provided `com.hypixel.hytale:Server` artifact and includes its UI assets in the mod jar.

## Installation

1. Build the mod using the included Gradle wrapper:

   ```bash
   # Unix / macOS / Git Bash
   ./gradlew build

   # Windows cmd / PowerShell
   gradlew.bat build
   ```

2. Use the generated jar:

   ```text
   build/libs/PrefabOptimizer-Extended-0.2.0-SNAPSHOT.jar
   ```

3. Install or load the jar in your Hytale server mods environment.

## Commands

| Command | Aliases | Description |
| --- | --- | --- |
| `/prefaboptimizer` | `/popt`, `/prefabopt` | Opens the prefab batch optimizer GUI. |
| `/prefaboptimizer prefabs` | `/prefaboptimizer prefab`, `/prefaboptimizer batch` | Opens the prefab batch optimizer GUI. |
| `/prefaboptimizer selection` | `/prefaboptimizer sel` | Opens the selection optimizer GUI if the player has a BuilderTools selection. |

Commands are intentionally unchanged from the original so that existing muscle memory keeps working.

## Selection Optimizer

Use this when you want to optimize blocks already placed in a world or prefab editor selection.

1. Create a BuilderTools selection.
2. Run `/prefaboptimizer selection`.
3. Choose optimizer settings.
4. Click `Optimize Selection`.
5. The GUI closes and a chat notification confirms that optimization is in process.
6. When it finishes, PrefabOptimizer-Extended reports the number and percentage of blocks removed.
7. Use `/undo` if you need to revert the edit.

The selection optimizer checks the current BuilderTools selection, counts hidden removable blocks, and uses BuilderTools' replace operation with a custom mask. This keeps the change compatible with BuilderTools history.

## Prefab Batch Optimizer

Use this when you want to generate optimized copies of prefab files.

1. Run `/prefaboptimizer`.
2. Browse prefab files and folders in the prefab browser.
3. Select individual prefabs, or open a folder and click `Add Current Folder`.
4. Enable or disable `Search selected folders recursively`.
5. Choose a writable asset-only target mod.
6. Choose the target folder inside `Server/Prefabs`.
7. Choose optimizer settings.
8. Click `Optimize Prefab Batch`.
9. The GUI closes and a chat notification confirms that optimization is in process.
10. When it finishes, PrefabOptimizer-Extended reports the number and percentage of blocks removed.

The batch optimizer saves optimized copies into the target mod. It does not overwrite source prefabs unless you deliberately choose the same target pack/folder path.

## Optimizer Settings

### Preserve transparent / non-opaque blocks

Enabled by default.

When enabled, blocks whose Hytale opacity is not solid are never removed. This protects glass, water-like blocks, leaves, decorative transparent blocks, and other blocks that may matter visually even when surrounded.

### Strict cube only

Enabled by default.

When enabled, only `DrawType.Cube` blocks can be removed. When disabled, `DrawType.CubeWithModel` can also be considered if the block is otherwise solid and not excluded.

### Preserve blocks next to water / lava

Enabled by default.

When enabled, any block whose six-direct-neighbor position contains a fluid voxel is preserved even if the block itself would otherwise be classified as removable. This keeps pond floors, aquarium walls, lava channels, and other fluid containers intact. Disable this only if you are sure your prefab has no fluids, or if you explicitly want to strip them.

### Flood-fill interior mode

Disabled by default. More aggressive than the standard mode — opt in when you want maximum compression.

The default mode only removes blocks whose six direct neighbors are all themselves optimizable full-cube blocks. That leaves a lot of hidden volume behind: any block next to a small air pocket, a curved wall, or a sloped surface fails the strict six-neighbor test even when no player could ever see it.

Flood-fill mode runs a 3D BFS starting from the exterior of the selection/prefab bounding box. It propagates through every position that is not an optimizable occluder — air, glass, transparent decorative blocks, fluids, and so on. Any optimizable block whose six neighbors are all *unreachable* from the exterior is considered interior and removed.

This correctly strips:

- Thick walls with irregular or sloped interior surfaces.
- Hollow pockets that are fully enclosed (the pocket stays, but the shell gets thinner).
- Dense solid masses (mountains, cliffs, large statues) where six-neighbor left one-block-thick inner shells.

The `Preserve blocks next to water / lava` and `Exclude blocks` settings still apply in this mode, so fluids and user-excluded blocks remain safe.

### Exclude blocks

Use this field to protect specific block IDs from removal.

If the input contains commas, semicolons, or newlines, it is treated as a case-insensitive token list. A block is excluded if its ID equals or contains any token.

```text
glass,water,leaves
```

If the input is a single expression without list separators, it is treated as a regex and matched with `find()`.

```text
^UnstableRifts_|Glass|Water
```

If the regex is invalid, the value falls back to token matching.

## Optimization Rules

A block is removable only when all of the following are true:

- The block ID is valid and known to Hytale.
- The block is solid material.
- The block is not excluded by the custom exclusion setting.
- The block passes the transparent/non-opaque setting.
- The block passes the draw type setting.
- The block is not a filler block.
- No neighbor position contains a fluid (when "Preserve blocks next to water / lava" is enabled).
- **Default mode:** all six direct neighbors are inside the selected/prefab bounds, and all six direct neighbors are also optimizable full-cube blocks.
- **Flood-fill interior mode:** none of the six neighbors are reachable from the exterior of the bounding box through non-occluding positions (air, transparent blocks, fluids, etc.).

The default mode is intentionally conservative and avoids removing exposed or structurally important visible blocks. Flood-fill interior mode removes strictly more blocks than default mode and is gated behind a checkbox.

## Project Structure

```text
src/main/java/dev/lewai/prefaboptimizerextended
|-- commands
|   |-- PrefabOptimizerCommand.java
|   |-- CommandPages.java
|   `-- subcommands
|-- optimization
|   |-- PrefabOptimizerService.java
|   |-- SelectionOptimizationService.java
|   |-- PrefabBatchOptimizer.java
|   |-- PrefabSourceCollector.java
|   |-- OccludedBlockMask.java
|   |-- BlockClassifier.java
|   `-- OptimizerSettings.java
|-- ui
|   |-- PrefabOptimizerPage.java
|   |-- SelectionOptimizerPage.java
|   |-- OptimizerEventData.java
|   `-- SelectionEventData.java
`-- util
```

UI layouts live under:

```text
src/main/resources/Common/UI/Custom/Pages/PrefabOptimizer
```

## Development

Compile without running tests:

```bash
./gradlew compileJava
```

Build a jar:

```bash
./gradlew build
```

Show deprecation and other compiler warnings:

```bash
./gradlew compileJava -Pcompiler.showDeprecation=true -Pcompiler.showWarnings=true
```

Clean:

```bash
./gradlew clean
```

## Compatibility Notes

Some BuilderTools APIs differ across Hytale builds. PrefabOptimizer-Extended inherits the original compatibility shims for:

- BuilderTools selection permission checks whose player parameter type differs between builds.
- Selection bounds whose vector type may differ between compile-time and runtime APIs.
- BuilderTools block mask integration, which still requires Hytale's deprecated `ChunkAccessor` signature.

These shims are intentionally narrow and isolated in the optimization layer.

## Known Limitations

- The default optimization mode is conservative and only removes blocks fully enclosed on all six direct faces.
- Batch prefab optimization is not undoable through `/undo`; it writes optimized prefab copies to a target mod.
- Selection optimization is undoable because it goes through BuilderTools history.
- The target mod list intentionally excludes read-only and Java-backed mods; it only allows writable asset-only mods.
- Very large prefab batches may still take time, but they are processed on a background executor instead of the UI/world thread.

## Credits

- Original mod: **Theobosse** ([ninesliced.com](https://ninesliced.com)) — creator of PrefabOptimizer. All core algorithms, BuilderTools integration, and UI pages are their work.
- Extended fork: **lewai1** ([github.com/lewai1](https://github.com/lewai1)) — performance, UX, and additional optimization modes.

## License

PrefabOptimizer-Extended is licensed under the **GNU Affero General Public License v3.0**, matching the upstream PrefabOptimizer project.

Because this mod is AGPL, any deployment that exposes it over a network (including running it on a Hytale server reachable by players) must make the corresponding source code available to its users.

See [`LICENSE.md`](./LICENSE.md).
