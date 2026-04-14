# PrefabOptimizer

PrefabOptimizer is a Hytale builder utility mod that removes hidden, fully enclosed blocks from BuilderTools selections and prefab files. It is meant to turn an internal optimization script into a practical in-game tool that builders and mod creators can use without leaving Hytale.

The optimizer keeps visible surfaces, entities, fluids, tints, and block component holders intact while removing blocks that are completely surrounded on all six faces by optimizable full-cube blocks.

## Features

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
- Maven.
- Creative-mode permission for the commands.

The mod depends on Hytale's provided `com.hypixel.hytale:Server` artifact and includes its UI assets in the mod jar.

## Installation

1. Build the mod:

   ```bash
   mvn clean install
   ```

2. Use the generated jar:

   ```text
   target/PrefabOptimizer-0.1.0.jar
   ```

3. Install or load the jar in your Hytale server mods environment.

## Commands

| Command | Aliases | Description |
| --- | --- | --- |
| `/prefaboptimizer` | `/popt`, `/prefabopt` | Opens the prefab batch optimizer GUI. |
| `/prefaboptimizer prefabs` | `/prefaboptimizer prefab`, `/prefaboptimizer batch` | Opens the prefab batch optimizer GUI. |
| `/prefaboptimizer selection` | `/prefaboptimizer sel` | Opens the selection optimizer GUI if the player has a BuilderTools selection. |

## Selection Optimizer

Use this when you want to optimize blocks already placed in a world or prefab editor selection.

1. Create a BuilderTools selection.
2. Run `/prefaboptimizer selection`.
3. Choose optimizer settings.
4. Click `Optimize Selection`.
5. The GUI closes and a chat notification confirms that optimization is in process.
6. When it finishes, PrefabOptimizer reports the number and percentage of blocks removed.
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
10. When it finishes, PrefabOptimizer reports the number and percentage of blocks removed.

The batch optimizer saves optimized copies into the target mod. It does not overwrite source prefabs unless you deliberately choose the same target pack/folder path.

## Optimizer Settings

### Preserve transparent / non-opaque blocks

Enabled by default.

When enabled, blocks whose Hytale opacity is not solid are never removed. This protects glass, water-like blocks, leaves, decorative transparent blocks, and other blocks that may matter visually even when surrounded.

### Strict cube only

Enabled by default.

When enabled, only `DrawType.Cube` blocks can be removed. When disabled, `DrawType.CubeWithModel` can also be considered if the block is otherwise solid and not excluded.

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
- All six direct neighbors are inside the selected/prefab bounds.
- All six direct neighbors are also optimizable full-cube blocks.

This intentionally conservative first mode avoids removing exposed or structurally important visible blocks.

## Project Structure

```text
src/main/java/dev/ninesliced/prefaboptimizer
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
mvn -DskipTests compile
```

Build and install locally:

```bash
mvn clean install
```

Check source deprecation warnings:

```bash
mvn -DskipTests -Dmaven.compiler.showDeprecation=true -Dmaven.compiler.showWarnings=true clean compile
```

The Maven startup warning about `sun.misc.Unsafe` comes from Maven/Guava on the current toolchain, not from PrefabOptimizer source code.

## Compatibility Notes

Some BuilderTools APIs differ across Hytale builds. PrefabOptimizer uses small compatibility shims for:

- BuilderTools selection permission checks whose player parameter type differs between builds.
- Selection bounds whose vector type may differ between compile-time and runtime APIs.
- BuilderTools block mask integration, which still requires Hytale's deprecated `ChunkAccessor` signature.

These shims are intentionally narrow and isolated in the optimization layer.

## Known Limitations

- The current optimization mode is conservative and only removes blocks fully enclosed on all six direct faces.
- Batch prefab optimization is not undoable through `/undo`; it writes optimized prefab copies to a target mod.
- Selection optimization is undoable because it goes through BuilderTools history.
- The target mod list intentionally excludes read-only and Java-backed mods; it only allows writable asset-only mods.
- Very large prefab batches may still take time, but they are processed on a background executor instead of the UI/world thread.

## License

PrefabOptimizer is licensed under the GNU Affero General Public License v3.0, matching Hytale-BetterMap.

See `LICENSE.md`.
