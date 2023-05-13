package io.th0rgal.oraxen;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.ticxo.playeranimator.PlayerAnimatorImpl;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIConfig;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.events.OraxenItemsLoadedEvent;
import io.th0rgal.oraxen.commands.CommandsManager;
import io.th0rgal.oraxen.compatibilities.CompatibilitiesManager;
import io.th0rgal.oraxen.config.ConfigsManager;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.config.SettingsUpdater;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.font.packets.InventoryPacketListener;
import io.th0rgal.oraxen.font.packets.TitlePacketListener;
import io.th0rgal.oraxen.gestures.GestureManager;
import io.th0rgal.oraxen.hud.HudManager;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.pack.generation.ResourcePack;
import io.th0rgal.oraxen.pack.upload.UploadManager;
import io.th0rgal.oraxen.recipes.RecipesManager;
import io.th0rgal.oraxen.sound.SoundManager;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.OS;
import io.th0rgal.oraxen.utils.actions.ClickActionManager;
import io.th0rgal.oraxen.utils.armorequipevent.ArmorListener;
import io.th0rgal.oraxen.utils.breaker.BreakerSystem;
import io.th0rgal.oraxen.utils.customarmor.CustomArmorListener;
import io.th0rgal.oraxen.utils.inventories.InvManager;
import io.th0rgal.oraxen.utils.logs.Logs;
import io.th0rgal.protectionlib.ProtectionLib;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class OraxenPlugin extends JavaPlugin {

    private static OraxenPlugin oraxen;
    private static GestureManager gestureManager;
    private ConfigsManager configsManager;
    private BukkitAudiences audience;
    private UploadManager uploadManager;
    private FontManager fontManager;
    private HudManager hudManager;
    private SoundManager soundManager;
    private InvManager invManager;
    private ResourcePack resourcePack;
    private ClickActionManager clickActionManager;
    private ProtocolManager protocolManager;
    public final boolean isPaperServer;
    public static boolean supportsDisplayEntities;

    public OraxenPlugin() throws NoSuchFieldException, IllegalAccessException {
        oraxen = this;
        isPaperServer = checkIfPaperServer();
        supportsDisplayEntities = checkIfSupportsDisplayEntities();
        Logs.enableFilter();
    }

    private static boolean checkIfPaperServer() {
        try {
            Class.forName("com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean checkIfSupportsDisplayEntities() {
        try {
            Class.forName("org.bukkit.entity.ItemDisplay");
            if (Bukkit.getPluginManager().isPluginEnabled("ViaBackwards") && FurnitureFactory.getInstance().detectViabackwards) {
                Logs.logWarning("ViaBackwards is installed, disabling Display Entity type for Furniture");
                Logs.logWarning("Display Entity furniture is entirely invisible and uninteractable for players using 1.19.3 or lower");
                Logs.logWarning("If you still want to use Display Entity type for Furniture, disable detect_viabackwards in the mechanics.yml");
                return false;
            }
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static OraxenPlugin get() {
        return oraxen;
    }

    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIConfig().silentLogs(true));
    }

    @Override
    public void onEnable() {
        CommandAPI.onEnable(this);
        ProtectionLib.init(this);
        PlayerAnimatorImpl.initialize(this);
        audience = BukkitAudiences.create(this);
        clickActionManager = new ClickActionManager(this);
        reloadConfigs();
        if (Settings.KEEP_UP_TO_DATE.toBool())
            new SettingsUpdater().handleSettingsUpdate();
        final PluginManager pluginManager = Bukkit.getPluginManager();
        if (ProtocolLibrary.getPlugin().isEnabled()) {
            protocolManager = ProtocolLibrary.getProtocolManager();
            new BreakerSystem().registerListener();
            if (Settings.FORMAT_INVENTORY_TITLES.toBool())
                protocolManager.addPacketListener(new InventoryPacketListener());
            protocolManager.addPacketListener(new TitlePacketListener());
        } else Logs.logWarning("ProtocolLib is not on your server, some features will not work");
        if (Settings.DISABLE_LEATHER_REPAIR_CUSTOM.toBool())
            pluginManager.registerEvents(new CustomArmorListener(), this);
        resourcePack = new ResourcePack(this);
        MechanicsManager.registerNativeMechanics();
        //CustomBlockData.registerListener(this); //Handle this manually
        hudManager = new HudManager(configsManager);
        fontManager = new FontManager(configsManager);
        soundManager = new SoundManager(configsManager.getSound());
        gestureManager = new GestureManager();
        OraxenItems.loadItems(configsManager);
        fontManager.registerEvents();
        fontManager.verifyRequired(); // Verify the required glyph is there
        hudManager.registerEvents();
        hudManager.registerTask();
        hudManager.parsedHudDisplays = hudManager.generateHudDisplays();
        pluginManager.registerEvents(new ItemUpdater(), this);
        resourcePack.generate(fontManager, soundManager);
        RecipesManager.load(this);
        invManager = new InvManager();
        new ArmorListener(Settings.ARMOR_EQUIP_EVENT_BYPASS.toStringList()).registerEvents(this);
        new CommandsManager().loadCommands();
        postLoading(configsManager);
        try {
            Message.PLUGIN_LOADED.log(AdventureUtils.tagResolver("os", OS.getOs().getPlatformName()));
        } catch (Exception ignore) {
        }
        CompatibilitiesManager.enableNativeCompatibilities();
    }

    private void postLoading(final ConfigsManager configsManager) {
        uploadManager = new UploadManager(this);
        uploadManager.uploadAsyncAndSendToPlayers(resourcePack);
        new Metrics(this, 5371);
        Bukkit.getScheduler().runTask(this, () -> {
            //TODO Is this needed?
            //OraxenItems.loadItems(configsManager);
            Bukkit.getPluginManager().callEvent(new OraxenItemsLoadedEvent());
        });
    }

    @Override
    public void onDisable() {
        unregisterListeners();
        CompatibilitiesManager.disableCompatibilities();
        Message.PLUGIN_UNLOADED.log();
    }

    private void unregisterListeners() {
        fontManager.unregisterEvents();
        hudManager.unregisterEvents();
        MechanicsManager.unloadListeners();
        HandlerList.unregisterAll(this);
    }

    public ProtocolManager getProtocolManager() {
        return protocolManager;
    }

    public GestureManager getGesturesManager() {
        return gestureManager;
    }

    public BukkitAudiences getAudience() {
        return audience;
    }

    public void reloadConfigs() {
        configsManager = new ConfigsManager(this);
        if (!configsManager.validatesConfig()) {
            Logs.logError("unable to validate config");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    public ConfigsManager getConfigsManager() {
        return configsManager;
    }

    public UploadManager getUploadManager() {
        return uploadManager;
    }

    public FontManager getFontManager() {
        return fontManager;
    }

    public void setFontManager(final FontManager fontManager) {
        this.fontManager.unregisterEvents();
        this.fontManager = fontManager;
        fontManager.registerEvents();
    }

    public HudManager getHudManager() {
        return hudManager;
    }

    public void setHudManager(final HudManager hudManager) {
        this.hudManager.unregisterEvents();
        this.hudManager = hudManager;
        hudManager.registerEvents();
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

}
