package dev.lewai.prefaboptimizerextended.ui;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.browser.FileListProvider;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.lewai.prefaboptimizerextended.optimization.OptimizerSettings;
import dev.lewai.prefaboptimizerextended.optimization.PrefabOptimizationResult;
import dev.lewai.prefaboptimizerextended.optimization.PrefabOptimizerService;
import dev.lewai.prefaboptimizerextended.util.ThrowableMessages;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nonnull;

public final class PrefabOptimizerPage extends InteractiveCustomUIPage<OptimizerEventData> {
    private static final String LAYOUT_PATH = "Pages/PrefabOptimizer/PrefabOptimizer.ui";
    private static final String FILE_LIST_PATH = "#FileList";
    private static final String SELECTED_LIST_PATH = "#SelectedSourceList";
    private static final Value<String> SELECTED_BUTTON_STYLE = Value.ref("Pages/BasicTextButton.ui", "SelectedLabelStyle");

    private final Set<String> selectedSources = new LinkedHashSet<>();
    private Path sourceCurrentDir = Paths.get("");
    private String sourceSearchQuery = "";
    private String status = "Choose prefabs or folders, select the target asset-only mod, then optimize the batch.";
    private volatile boolean batchRunning = false;

    public PrefabOptimizerPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, OptimizerEventData.CODEC);
    }

    @Override
    public void build(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull UICommandBuilder ui,
        @Nonnull UIEventBuilder events,
        @Nonnull Store<EntityStore> store
    ) {
        ui.append(LAYOUT_PATH);
        this.bindStaticActions(events);
        this.buildSourceBrowser(ui, events);
        this.buildSelectedSourceList(ui, events);
        this.buildTargetPackDropdown(ui);
        this.applyStatus(ui);
    }

    @Override
    public void handleDataEvent(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull OptimizerEventData data
    ) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        if (data.searchQuery != null) {
            this.sourceSearchQuery = data.searchQuery.trim().toLowerCase(Locale.ROOT);
            this.sendSourceUpdate();
            return;
        }
        if (data.file != null || data.searchResult != null) {
            this.handleSourceBrowserEvent(data);
            this.sendSourceUpdate();
            return;
        }

        Action action = UiActions.parse(Action.class, data.action);
        if (action == null) {
            return;
        }

        switch (action) {
            case CLOSE -> player.getPageManager().setPage(ref, store, Page.None);
            case ADD_CURRENT_FOLDER -> {
                this.addCurrentFolder();
                this.sendSourceUpdate();
            }
            case CLEAR_SOURCES -> {
                this.selectedSources.clear();
                this.status = "Source list cleared.";
                this.sendSourceUpdate();
            }
            case REMOVE_SOURCE -> {
                if (data.source != null) {
                    this.selectedSources.remove(data.source);
                    this.status = "Removed source: " + data.source;
                }
                this.sendSourceUpdate();
            }
            case OPTIMIZE_PREFABS -> {
                if (this.optimizeSelectedPrefabs(data)) {
                    player.getPageManager().setPage(ref, store, Page.None);
                } else {
                    this.sendStatusUpdate();
                }
            }
        }
    }

    private void bindStaticActions(@Nonnull UIEventBuilder events) {
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", actionData(Action.CLOSE), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#AddFolderButton", actionData(Action.ADD_CURRENT_FOLDER), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ClearSourcesButton", actionData(Action.CLEAR_SOURCES), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#OptimizePrefabsButton", settingsActionData(Action.OPTIMIZE_PREFABS), false);
    }

    private void buildSourceBrowser(@Nonnull UICommandBuilder ui, @Nonnull UIEventBuilder events) {
        ui.set("#CurrentPath.Text", this.currentPathLabel());
        ui.set("#SearchInput.Value", this.sourceSearchQuery);
        events.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#SearchInput",
            EventData.of(OptimizerEventData.KEY_SEARCH_QUERY, "#SearchInput.Value"),
            false
        );

        ui.clear(FILE_LIST_PATH);
        int index = 0;
        if (!this.sourceCurrentDir.toString().isEmpty() && this.sourceSearchQuery.isEmpty()) {
            appendFileEntry(ui, events, index++, "../", "..", false, false);
        }

        List<FileListProvider.FileEntry> entries = PrefabOptimizerService.sourceProvider().getFiles(this.sourceCurrentDir, this.sourceSearchQuery);
        for (FileListProvider.FileEntry entry : entries) {
            boolean searchResult = !this.sourceSearchQuery.isEmpty() && !entry.isDirectory();
            String text = entry.isDirectory() ? entry.displayName() + "/" : entry.displayName();
            appendFileEntry(ui, events, index++, text, entry.name(), entry.isDirectory(), searchResult);
        }
    }

    private void appendFileEntry(
        @Nonnull UICommandBuilder ui,
        @Nonnull UIEventBuilder events,
        int index,
        @Nonnull String text,
        @Nonnull String eventValue,
        boolean directory,
        boolean searchResult
    ) {
        String itemPath = FILE_LIST_PATH + "[" + index + "]";
        ui.append(FILE_LIST_PATH, "Pages/BasicTextButton.ui");
        ui.set(itemPath + ".Text", text);
        if (!directory) {
            ui.set(itemPath + ".Style", SELECTED_BUTTON_STYLE);
        }
        EventData data = searchResult
            ? EventData.of(OptimizerEventData.KEY_SEARCH_RESULT, eventValue)
            : EventData.of(OptimizerEventData.KEY_FILE, eventValue);
        events.addEventBinding(CustomUIEventBindingType.Activating, itemPath, data, false);
    }

    private void buildSelectedSourceList(@Nonnull UICommandBuilder ui, @Nonnull UIEventBuilder events) {
        ui.clear(SELECTED_LIST_PATH);
        ui.set("#SelectedCount.Text", this.selectedSources.size() + " selected");
        int index = 0;
        if (this.selectedSources.isEmpty()) {
            ui.append(SELECTED_LIST_PATH, "Pages/BasicTextButton.ui");
            ui.set(SELECTED_LIST_PATH + "[0].Text", "No sources selected yet");
            return;
        }
        for (String source : this.selectedSources) {
            String itemPath = SELECTED_LIST_PATH + "[" + index + "]";
            ui.append(SELECTED_LIST_PATH, "Pages/BasicTextButton.ui");
            ui.set(itemPath + ".Text", "Remove: " + source);
            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                itemPath,
                actionData(Action.REMOVE_SOURCE).put(OptimizerEventData.KEY_SOURCE, source),
                false
            );
            index++;
        }
    }

    private void buildTargetPackDropdown(@Nonnull UICommandBuilder ui) {
        ObjectArrayList<DropdownEntryInfo> entries = new ObjectArrayList<>();
        String firstValue = "";
        for (AssetPack pack : writableAssetOnlyPacks()) {
            if (firstValue.isEmpty()) {
                firstValue = pack.getName();
            }
            entries.add(new DropdownEntryInfo(LocalizableString.fromString(pack.getManifest().getName()), pack.getName()));
        }
        if (entries.isEmpty()) {
            entries.add(new DropdownEntryInfo(LocalizableString.fromString("No writable asset-only mods"), ""));
        }
        ui.set("#TargetPack.Entries", entries);
        ui.set("#TargetPack.Value", firstValue);
        ui.set("#TargetFolder.Value", "Optimized");
    }

    private void applyStatus(@Nonnull UICommandBuilder ui) {
        ui.set("#StatusLabel.Text", this.status);
        ui.set("#OptimizePrefabsButton.Disabled", this.batchRunning);
        ui.set("#OptimizePrefabsButton.Text", this.batchRunning ? "Optimizing Prefab Batch..." : "Optimize Prefab Batch");
    }

    private void handleSourceBrowserEvent(@Nonnull OptimizerEventData data) {
        String selectedPath = data.searchResult != null ? data.searchResult : data.file;
        if (selectedPath == null) {
            return;
        }
        if ("..".equals(selectedPath)) {
            Path parent = this.sourceCurrentDir.getParent();
            this.sourceCurrentDir = parent == null ? Paths.get("") : parent;
            this.status = "Moved up to " + this.currentPathLabel() + ".";
            return;
        }

        String virtualPath = data.searchResult != null ? selectedPath : joinVirtualPath(this.sourceCurrentDir, selectedPath);
        Path resolved = PrefabOptimizerService.sourceProvider().resolveVirtualPath(virtualPath);
        if (resolved == null) {
            this.status = "Could not resolve source: " + virtualPath;
            return;
        }
        if (Files.isDirectory(resolved, LinkOption.NOFOLLOW_LINKS)) {
            this.sourceCurrentDir = Paths.get(virtualPath);
            this.sourceSearchQuery = "";
            this.status = "Browsing " + this.currentPathLabel() + ".";
            return;
        }

        this.selectedSources.add(virtualPath);
        this.status = "Added prefab: " + virtualPath;
    }

    private void addCurrentFolder() {
        String virtualPath = this.sourceCurrentDir.toString().replace('\\', '/');
        if (virtualPath.isBlank()) {
            this.status = "Open a pack or folder first, then add the current folder.";
            return;
        }
        Path resolved = PrefabOptimizerService.sourceProvider().resolveVirtualPath(virtualPath);
        if (resolved == null || !Files.isDirectory(resolved, LinkOption.NOFOLLOW_LINKS)) {
            this.status = "The current path is not a folder: " + virtualPath;
            return;
        }
        this.selectedSources.add(virtualPath);
        this.status = "Added folder: " + virtualPath;
    }

    private boolean optimizeSelectedPrefabs(@Nonnull OptimizerEventData data) {
        if (this.batchRunning) {
            this.status = "A prefab batch optimization is already running.";
            return false;
        }
        if (this.selectedSources.isEmpty()) {
            this.status = "Select at least one prefab or folder first.";
            return false;
        }
        AssetPack targetPack = data.targetPack == null || data.targetPack.isBlank()
            ? null
            : AssetModule.get().getAssetPack(data.targetPack);
        if (targetPack == null || !isWritableAssetOnlyPack(targetPack)) {
            this.status = "Choose a writable asset-only target mod first.";
            return false;
        }
        String targetFolder = data.targetFolder == null || data.targetFolder.isBlank() ? "Optimized" : data.targetFolder.trim();
        OptimizerSettings settings = data.toSettings();
        List<String> sources = List.copyOf(this.selectedSources);

        this.batchRunning = true;
        this.status = "Prefab batch optimization is in process for " + sources.size()
            + " selected prefab source" + (sources.size() == 1 ? "" : "s") + ".";
        this.playerRef.sendMessage(Message.raw("PrefabOptimizer-Extended: " + this.status));

        PrefabOptimizerService.optimizePrefabSourcesAsync(
            sources,
            targetPack,
            targetFolder,
            settings,
            defaultTrue(data.recursiveFolders)
        ).whenComplete((result, throwable) -> this.handleBatchFinished(targetPack, result, throwable));
        return true;
    }

    private void handleBatchFinished(
        @Nonnull AssetPack targetPack,
        PrefabOptimizationResult result,
        Throwable throwable
    ) {
        this.batchRunning = false;
        if (throwable != null) {
            this.status = "Prefab batch failed: " + ThrowableMessages.readableMessage(ThrowableMessages.rootCause(throwable));
            this.playerRef.sendMessage(Message.raw("PrefabOptimizer-Extended: " + this.status));
            return;
        }

        this.status = "Saved " + result.savedCount() + "/" + result.sourceCount()
            + " prefabs to " + targetPack.getName()
            + " and removed " + result.removedBlocks() + "/" + result.processedBlocks()
            + " blocks (" + formatPercentage(result.removedBlockPercentage()) + " optimized).";
        this.playerRef.sendMessage(Message.raw("PrefabOptimizer-Extended: " + this.status));
        for (String warning : result.warnings()) {
            this.playerRef.sendMessage(Message.raw("PrefabOptimizer-Extended warning: " + warning));
        }
    }

    private void sendSourceUpdate() {
        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();
        this.bindStaticActions(events);
        this.buildSourceBrowser(ui, events);
        this.buildSelectedSourceList(ui, events);
        this.applyStatus(ui);
        this.sendUpdate(ui, events, false);
    }

    private void sendStatusUpdate() {
        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();
        this.bindStaticActions(events);
        this.applyStatus(ui);
        this.sendUpdate(ui, events, false);
    }

    @Nonnull
    private String currentPathLabel() {
        String currentDir = this.sourceCurrentDir.toString().replace('\\', '/');
        if (currentDir.isEmpty()) {
            return "Assets";
        }
        String[] parts = currentDir.split("/", 2);
        if ("HytaleAssets".equals(parts[0])) {
            return currentDir;
        }
        return "Mods/" + currentDir;
    }

    @Nonnull
    private static String joinVirtualPath(@Nonnull Path currentDir, @Nonnull String child) {
        String base = currentDir.toString().replace('\\', '/');
        return base.isEmpty() ? child : base + "/" + child;
    }

    @Nonnull
    private static EventData actionData(@Nonnull Action action) {
        return new EventData().put(OptimizerEventData.KEY_ACTION, action.name());
    }

    @Nonnull
    private static EventData settingsActionData(@Nonnull Action action) {
        return actionData(action)
            .put(OptimizerEventData.KEY_PRESERVE_TRANSPARENT, "#PreserveTransparent.Value")
            .put(OptimizerEventData.KEY_STRICT_CUBE_ONLY, "#StrictCubeOnly.Value")
            .put(OptimizerEventData.KEY_EXCLUDED_BLOCKS, "#ExcludedBlocks.Value")
            .put(OptimizerEventData.KEY_RECURSIVE_FOLDERS, "#RecursiveFolders.Value")
            .put(OptimizerEventData.KEY_TARGET_PACK, "#TargetPack.Value")
            .put(OptimizerEventData.KEY_TARGET_FOLDER, "#TargetFolder.Value");
    }

    @Nonnull
    private static List<AssetPack> writableAssetOnlyPacks() {
        ObjectArrayList<AssetPack> packs = new ObjectArrayList<>();
        for (AssetPack pack : AssetModule.get().getAssetPacks()) {
            if (isWritableAssetOnlyPack(pack)) {
                packs.add(pack);
            }
        }
        return packs;
    }

    private static boolean isWritableAssetOnlyPack(@Nonnull AssetPack pack) {
        PluginManifest manifest = pack.getManifest();
        String main = manifest != null ? manifest.getMain() : null;
        return !pack.isImmutable()
            && manifest != null
            && (main == null || main.isBlank());
    }

    private static boolean defaultTrue(Boolean value) {
        return value == null || value;
    }

    @Nonnull
    private static String formatPercentage(double percentage) {
        return String.format(Locale.ROOT, "%.2f%%", percentage);
    }

    private enum Action {
        CLOSE,
        ADD_CURRENT_FOLDER,
        CLEAR_SOURCES,
        REMOVE_SOURCE,
        OPTIMIZE_PREFABS
    }
}
