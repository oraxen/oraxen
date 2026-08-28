package io.th0rgal.oraxen;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.events.OraxenItemsLoadedEvent;
import io.th0rgal.oraxen.commands.CommandsManager;
import io.th0rgal.oraxen.commands.OraxenCommand;
import io.th0rgal.oraxen.compatibilities.CompatibilitiesManager;
import io.th0rgal.oraxen.configs.ConfigsManager;
import io.th0rgal.oraxen.configs.Message;
import io.th0rgal.oraxen.configs.ResourcesManager;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.configs.SettingsUpdater;
import io.th0rgal.oraxen.fonts.FontManager;
import io.th0rgal.oraxen.hopper.OraxenHopper;
import io.th0rgal.oraxen.introduction.IntroductionGuide;
import io.th0rgal.oraxen.packets.PacketAdapter;
import io.th0rgal.oraxen.packets.PacketEventsAdapter;
import io.th0rgal.oraxen.packets.ProtocolLibAdapter;
import io.th0rgal.oraxen.hud.HudManager;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.pack.dispatch.PackLoadingManager;
import io.th0rgal.oraxen.pack.generation.PackVersionManager;
import io.th0rgal.oraxen.paintings.CustomPainting;
import io.th0rgal.oraxen.paintings.CustomPaintingListener;
import io.th0rgal.oraxen.paintings.CustomPaintingRegistry;
import io.th0rgal.oraxen.pack.generation.ResourcePack;
import io.th0rgal.oraxen.pack.upload.UploadManager;
import io.th0rgal.oraxen.recipes.builders.RecipeBuilder;
import io.th0rgal.oraxen.recipes.RecipesManager;
import io.th0rgal.oraxen.sounds.CustomJukeboxSongRegistry;
import io.th0rgal.oraxen.sounds.SoundManager;
import io.th0rgal.oraxen.utils.*;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.actions.ClickActionManager;
import io.th0rgal.oraxen.utils.armorequipevent.ArmorEquipEvent;
import io.th0rgal.oraxen.utils.breaker.BreakerSystem;
import io.th0rgal.oraxen.utils.breaker.CustomBlockMiningListener;
import io.th0rgal.oraxen.utils.customarmor.CustomArmorListener;
import io.th0rgal.oraxen.utils.inventories.InvManager;
import io.th0rgal.oraxen.utils.logs.Logs;
import io.th0rgal.oraxen.protection.AntiGriefLib;
import org.apache.commons.lang3.SystemUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.jar.JarFile;

public class OraxenPlugin extends JavaPlugin {

    private static OraxenPlugin oraxen;
    // These managers are reassigned on /oraxen reload, which runs on a region thread on
    // Folia; volatile ensures other region threads observe the new instances instead of
    // repopulating caches (e.g. the sticky per-enum Settings cache) from stale managers.
    private volatile ConfigsManager configsManager;
    private volatile ResourcesManager resourceManager;
    private volatile UploadManager uploadManager;
    private volatile io.th0rgal.oraxen.pack.upload.MultiVersionUploadManager multiVersionUploadManager;
    private volatile FontManager fontManager;
    private volatile HudManager hudManager;
    private volatile SoundManager soundManager;
    private volatile InvManager invManager;
    private volatile ResourcePack resourcePack;
    private volatile ClickActionManager clickActionManager;
    private volatile PacketAdapter packetAdapter;

    public OraxenPlugin() {
        oraxen = this;
        // Register dependencies with Hopper for auto-download
        OraxenHopper.register(this);
    }

    public static OraxenPlugin get() {
        return oraxen;
    }

    @Nullable
    public static JarFile getJarFile() {
        try {
            return new JarFile(oraxen.getFile());
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void onLoad() {
        // Download dependencies registered with Hopper
        OraxenHopper.download(this);
    }

    @Override
    public void onEnable() {
        if (!VersionUtil.isSupportedServer()) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        clickActionManager = new ClickActionManager(this);
        reloadConfigs();
        AntiGriefLib.setDebug(Settings.DEBUG.toBool());
        AntiGriefLib.init(this);

        if (Settings.KEEP_UP_TO_DATE.toBool())
            new SettingsUpdater().handleSettingsUpdate();
        if (PacketAdapter.isProtocolLibEnabled()) {
            if (Settings.DEBUG.toBool()) Logs.logInfo("ProtocolLib is enabled, using ProtocolLibAdapter.");
            packetAdapter = new ProtocolLibAdapter();
        } else if (PacketAdapter.isPacketEventsEnabled()) {
            if (Settings.DEBUG.toBool()) Logs.logInfo("PacketEvents is enabled, using PacketEventsAdapter.");
            packetAdapter = new PacketEventsAdapter();
        } else {
            Logs.logWarning("Neither ProtocolLib nor PacketEvents is enabled, using EmptyAdapter.");
            packetAdapter = new PacketAdapter.EmptyAdapter();
            Message.MISSING_PROTOCOLLIB.log();
        }
        packetAdapter.whenEnabled(adapter -> {
            if (Settings.FORMAT_INVENTORY_TITLES.toBool())
                packetAdapter.registerInventoryListener();
            packetAdapter.registerTitleListener();
        });

        Bukkit.getPluginManager().registerEvents(new CustomArmorListener(), this);
        Bukkit.getPluginManager().registerEvents(new BlockDataListener(this), this);
        // BreakerSystem cancels BlockDamageEvent for the blocks it manages (furniture barriers,
        // bedrock-break, and custom blocks on pre-1.20.5 servers); CustomBlockMiningListener
        // ignores cancelled events and handles custom blocks via BLOCK_BREAK_SPEED on 1.20.5+.
        Bukkit.getPluginManager().registerEvents(new BreakerSystem(), this);
        if (CustomBlockMiningListener.isSupported()) {
            Bukkit.getPluginManager().registerEvents(new CustomBlockMiningListener(), this);
        }
        NMSHandlers.setup();

        // Auto-update Paper config for block updates (noteblock, tripwire, chorus)
        var updatedSettings = PaperConfigUpdater.ensureAllBlockUpdatesDisabled();
        if (!updatedSettings.isEmpty()) {
            Logs.logSuccess("Auto-updated paper-global.yml: enabled " + String.join(", ", updatedSettings) + " (restart required)");
        }

        resourcePack = new ResourcePack();
        MechanicsManager.registerNativeMechanics();
        hudManager = new HudManager(configsManager);
        fontManager = new FontManager(configsManager);
        initializeSoundManager();
        // On 1.21.6+ jukebox songs are registered during bootstrap via RegistryEvents.JUKEBOX_SONG.
        // Earlier supported versions inject the songs into the live NMS registry at startup.
        if (!VersionUtil.atOrAbove("1.21.6"))
            CustomJukeboxSongRegistry.reload(soundManager.getJukeboxSounds());
        OraxenItems.loadItems();
        fontManager.registerEvents();
        fontManager.verifyRequired(); // Verify the required glyph is there
        hudManager.registerEvents();
        hudManager.registerTask();
        hudManager.parsedHudDisplays = hudManager.generateHudDisplays();
        Bukkit.getPluginManager().registerEvents(new ItemUpdater(), this);
        Bukkit.getPluginManager().registerEvents(new CustomPaintingListener(), this);
        Bukkit.getPluginManager().registerEvents(new PackLoadingManager(), this);
        io.th0rgal.oraxen.pack.generation.MultiVersionPackValidator.validateAndLogWarnings();
        resourcePack.generate();
        RecipesManager.load(this);
        invManager = new InvManager();
        if (!VersionUtil.atOrAbove("1.21.2"))
            ArmorEquipEvent.registerListener(this);
        new CommandsManager().loadCommands();
        postLoading();
        try {
            Message.PLUGIN_LOADED.log(AdventureUtils.tagResolver("os", SystemUtils.OS_NAME + " " + SystemUtils.OS_VERSION));
        } catch (Exception ignore) {
        }
        CompatibilitiesManager.enableNativeCompatibilities();
        if (VersionUtil.isCompiled())
            NoticeUtils.compileNotice();

        IntroductionGuide introductionGuide = new IntroductionGuide(this);
        Bukkit.getPluginManager().registerEvents(introductionGuide, this);
        introductionGuide.start();
    }

    private void postLoading() {
        OraxenMetrics.register(this);
        // Runs synchronous HTTP requests; keep it off the enable path so a stalled
        // connection cannot block startup.
        SchedulerUtil.runTaskAsync(this, () -> new LU().l());
        SchedulerUtil.runTask(this, () -> new OraxenItemsLoadedEvent().callEvent());

        // Auto-generate schema in debug mode (useful for CI/CD)
        if (Settings.DEBUG.toBool()) {
            SchedulerUtil.runTaskLater(this, 20L, () -> {
                io.th0rgal.oraxen.utils.schema.SchemaGenerator.generateAndSave();
            }); // Small delay to ensure everything is loaded
        }
    }

    @Override
    public void onDisable() {
        if (configsManager == null) {
            HandlerList.unregisterAll(this);
            OraxenCommand.unregisterAll();
            return;
        }

        cleanupRuntimeResources();
        HandlerList.unregisterAll(this);
        FurnitureFactory.unregisterEvolution();
        MechanicsManager.unregisterTasks();
        RecipeBuilder.clearAll();

        // Clean up backpack cosmetic entities to prevent ghost armor stands
        io.th0rgal.oraxen.mechanics.provided.cosmetic.backpack.BackpackCosmeticManager.getInstance().cleanup();

        CompatibilitiesManager.disableCompatibilities();
        OraxenCommand.unregisterAll();
        Message.PLUGIN_UNLOADED.log();
    }

    private void cleanupRuntimeResources() {
        setUploadManager(null);
        setMultiVersionUploadManager(null);
        setHudManager(null);
        if (resourcePack != null) {
            resourcePack.shutdown();
        }
    }

    public ResourcesManager getResourceManager() {
        return resourceManager;
    }

    public void reloadConfigs() {
        configsManager = new ConfigsManager(this);
        configsManager.validatesConfig();
        resourceManager = new ResourcesManager(this);
    }

    private void initializeSoundManager() {
        soundManager = new SoundManager(configsManager.getSound());
    }

    public void reloadCustomPaintings() {
        CustomPaintingRegistry.reload(CustomPainting.fromConfigSection(
                configsManager.getPaintings().getConfigurationSection("paintings")));
    }

    public void reloadCustomJukeboxSongs() {
        initializeSoundManager();
        CustomJukeboxSongRegistry.reload(soundManager.getJukeboxSounds());
    }

    public ConfigsManager getConfigsManager() {
        return configsManager;
    }

    public UploadManager getUploadManager() {
        return uploadManager;
    }

    public void setUploadManager(final UploadManager uploadManager) {
        UploadManager previousUploadManager = this.uploadManager;
        if (previousUploadManager != null && previousUploadManager != uploadManager) {
            previousUploadManager.unregister();
        }
        this.uploadManager = uploadManager;
    }

    public io.th0rgal.oraxen.pack.upload.MultiVersionUploadManager getMultiVersionUploadManager() {
        return multiVersionUploadManager;
    }

    public void setMultiVersionUploadManager(final io.th0rgal.oraxen.pack.upload.MultiVersionUploadManager multiVersionUploadManager) {
        if (this.multiVersionUploadManager != null && this.multiVersionUploadManager != multiVersionUploadManager) {
            this.multiVersionUploadManager.unregister();
        }
        this.multiVersionUploadManager = multiVersionUploadManager;
    }

    /**
     * Gets the pack URL, works in both single-pack and multi-version modes.
     * In multi-version mode, returns the server's default pack version URL.
     */
    public String getPackURL() {
        var mvManager = multiVersionUploadManager;
        if (mvManager != null) {
            var versionManager = mvManager.getVersionManager();
            if (versionManager != null) {
                var serverVersion = versionManager.getServerPackVersion();
                if (serverVersion != null) {
                    String url = serverVersion.getPackURL();
                    if (url != null) return url;
                }
            }
        }
        var um = uploadManager;
        if (um != null) {
            return um.getHostingProvider().getPackURL();
        }
        return null;
    }

    /**
     * Gets the pack SHA1 hash, works in both single-pack and multi-version modes.
     * In multi-version mode, returns the server's default pack version SHA1.
     */
    public String getPackSHA1() {
        var mvManager = multiVersionUploadManager;
        if (mvManager != null) {
            var versionManager = mvManager.getVersionManager();
            if (versionManager != null) {
                var serverVersion = versionManager.getServerPackVersion();
                if (serverVersion != null) {
                    String hex = serverVersion.getPackSHA1Hex();
                    if (hex != null) return hex;
                }
            }
        }
        var um = uploadManager;
        if (um != null) {
            return um.getHostingProvider().getOriginalSHA1();
        }
        return null;
    }

    public FontManager getFontManager() {
        return fontManager;
    }

    public void setFontManager(final FontManager fontManager) {
        FontManager previousFontManager = this.fontManager;
        if (previousFontManager != null) {
            previousFontManager.unregisterEvents();
        }
        this.fontManager = fontManager;
        if (fontManager != null) {
            fontManager.registerEvents();
        }
    }

    public HudManager getHudManager() {
        return hudManager;
    }

    public void setHudManager(final HudManager hudManager) {
        HudManager previousHudManager = this.hudManager;
        if (previousHudManager != null) {
            previousHudManager.unregisterTask();
            previousHudManager.unregisterEvents();
        }
        this.hudManager = hudManager;
        if (hudManager != null) {
            hudManager.registerEvents();
        }
    }

    public SoundManager getSoundManager() {
        return soundManager;
    }

    public void setSoundManager(final SoundManager soundManager) {
        this.soundManager = soundManager;
    }

    public InvManager getInvManager() {
        return invManager;
    }

    public ResourcePack getResourcePack() {
        return resourcePack;
    }

    public ClickActionManager getClickActionManager() {
        return clickActionManager;
    }

    public PacketAdapter getPacketAdapter() {
        return packetAdapter;
    }
}
