package io.th0rgal.oraxen;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.events.OraxenItemsLoadedEvent;
import io.th0rgal.oraxen.commands.CommandsManager;
import io.th0rgal.oraxen.compatibilities.CompatibilitiesManager;
import io.th0rgal.oraxen.config.*;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.font.packets.InventoryPacketListener;
import io.th0rgal.oraxen.font.packets.TitlePacketListener;
import io.th0rgal.oraxen.hud.HudManager;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.nms.GlyphHandlers;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.pack.PackGenerator;
import io.th0rgal.oraxen.pack.server.EmptyServer;
import io.th0rgal.oraxen.pack.server.OraxenPackServer;
import io.th0rgal.oraxen.recipes.RecipesManager;
import io.th0rgal.oraxen.sound.SoundManager;
import io.th0rgal.oraxen.utils.LU;
import io.th0rgal.oraxen.utils.NoticeUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.actions.ClickActionManager;
import io.th0rgal.oraxen.utils.armorequipevent.ArmorEquipEvent;
import io.th0rgal.oraxen.utils.breaker.BreakerManager;
import io.th0rgal.oraxen.utils.customarmor.CustomArmorListener;
import io.th0rgal.oraxen.utils.inventories.InvManager;
import io.th0rgal.oraxen.utils.logs.Logs;
import io.th0rgal.protectionlib.ProtectionLib;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;

public class OraxenPlugin extends JavaPlugin {

    private static OraxenPlugin oraxen;
    private ConfigsManager configsManager;
    private ResourcesManager resourceManager;
    private BukkitAudiences audience;
    private FontManager fontManager;
    private HudManager hudManager;
    private SoundManager soundManager;
    private InvManager invManager;
    private PackGenerator packGenerator;
    @Nullable private OraxenPackServer packServer;
    private ClickActionManager clickActionManager;
    private ProtocolManager protocolManager;
    private BreakerManager breakerManager;
    public static boolean supportsDisplayEntities;

    public OraxenPlugin() {
        oraxen = this;
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
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this).silentLogs(true));
    }

    @Override
    public void onEnable() {
        CommandAPI.onEnable();
        ProtectionLib.init(this);
        audience = BukkitAudiences.create(this);
        reloadConfigs();
        clickActionManager = new ClickActionManager(this);
        supportsDisplayEntities = VersionUtil.atOrAbove("1.19.4");
        hudManager = new HudManager(configsManager);
        fontManager = new FontManager(configsManager);
        soundManager = new SoundManager(configsManager.getSounds());
        breakerManager = new BreakerManager(new ConcurrentHashMap<>());

        if (Settings.KEEP_UP_TO_DATE.toBool())
            new SettingsUpdater().handleSettingsUpdate();
        final PluginManager pluginManager = Bukkit.getPluginManager();
        if (pluginManager.isPluginEnabled("ProtocolLib")) {
            protocolManager = ProtocolLibrary.getProtocolManager();
            if (Settings.FORMAT_INVENTORY_TITLES.toBool())
                protocolManager.addPacketListener(new InventoryPacketListener());
            protocolManager.addPacketListener(new TitlePacketListener());
        } else Logs.logWarning("ProtocolLib is not on your server, some features will not work");
        pluginManager.registerEvents(new CustomArmorListener(), this);
        NMSHandlers.setup();
        packGenerator = new PackGenerator();

        MechanicsManager.registerNativeMechanics();
        OraxenItems.loadItems();
        fontManager.registerEvents();
        fontManager.verifyRequired(); // Verify the required glyph is there
        hudManager.registerEvents();
        hudManager.registerTask();
        hudManager.parsedHudDisplays = hudManager.generateHudDisplays();
        pluginManager.registerEvents(new ItemUpdater(), this);
        RecipesManager.load(this);
        invManager = new InvManager();
        ArmorEquipEvent.registerListener(this);
        new CommandsManager().loadCommands();

        packGenerator.generatePack();
        packServer = OraxenPackServer.initializeServer();
        packServer.start();
        packServer.uploadPack();
        postLoading();
        CompatibilitiesManager.enableNativeCompatibilities();
        if (VersionUtil.isCompiled()) NoticeUtils.compileNotice();
        if (VersionUtil.isLeaked()) NoticeUtils.leakNotice();
    }

    private void postLoading() {
        new Metrics(this, 5371);
        new LU().l();
        Bukkit.getScheduler().runTask(this, () ->
                Bukkit.getPluginManager().callEvent(new OraxenItemsLoadedEvent()));
    }

    @Override
    public void onDisable() {
        if (packServer != null) packServer.stop();
        unregisterListeners();
        FurnitureFactory.unregisterEvolution();
        FurnitureFactory.instance.furniturePacketManager().removeAllHitboxes();
        for (Player player : Bukkit.getOnlinePlayers())
            if (GlyphHandlers.isNms()) NMSHandlers.getHandler().uninject(player);

        CompatibilitiesManager.disableCompatibilities();
        Message.PLUGIN_UNLOADED.log();
    }

    private void unregisterListeners() {
        fontManager.unregisterEvents();
        hudManager.unregisterEvents();
        MechanicsManager.unloadListeners();
        HandlerList.unregisterAll(this);
    }

    public Path packPath() {
        return getDataFolder().toPath().resolve("pack");
    }

    public ProtocolManager protocolManager() {
        return protocolManager;
    }

    public BukkitAudiences audience() {
        return audience;
    }

    public void reloadConfigs() {
        resourceManager = new ResourcesManager(this);
        configsManager = new ConfigsManager(this);
        configsManager.validatesConfig();
    }

    public ConfigsManager configsManager() {
        return configsManager;
    }
    public ResourcesManager resourceManager() {
        return resourceManager;
    }
    public void resourceManager(ResourcesManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    public FontManager fontManager() {
        return fontManager;
    }

    public void fontManager(final FontManager fontManager) {
        this.fontManager.unregisterEvents();
        this.fontManager = fontManager;
        fontManager.registerEvents();
    }

    public HudManager hudManager() {
        return hudManager;
    }

    public void hudManager(final HudManager hudManager) {
        this.hudManager.unregisterEvents();
        this.hudManager = hudManager;
        hudManager.registerEvents();
    }

    public SoundManager soundManager() {
        return soundManager;
    }

    public void soundManager(final SoundManager soundManager) {
        this.soundManager = soundManager;
    }

    public BreakerManager breakerManager() {
        return breakerManager;
    }

    public InvManager invManager() {
        return invManager;
    }

    public PackGenerator packGenerator() {
        return packGenerator;
    }

    public void packGenerator(PackGenerator packGenerator) {
        this.packGenerator = packGenerator;
    }

    public OraxenPackServer packServer() {
        return packServer != null ? packServer : new EmptyServer();
    }
    public void packServer(@Nullable OraxenPackServer server) {
        if (packServer != null) packServer.stop();
        packServer = server;
        if (packServer != null) {
            packServer.start();
            packServer.uploadPack();
        }
    }

    public ClickActionManager clickActionManager() {
        return clickActionManager;
    }
}
