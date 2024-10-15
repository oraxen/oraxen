package io.th0rgal.oraxen;

import com.jeff_media.customblockdata.CustomBlockData;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.commands.CommandsManager;
import io.th0rgal.oraxen.compatibilities.CompatibilitiesManager;
import io.th0rgal.oraxen.config.*;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.pack.PackGenerator;
import io.th0rgal.oraxen.pack.server.EmptyServer;
import io.th0rgal.oraxen.pack.server.OraxenPackServer;
import io.th0rgal.oraxen.recipes.RecipesManager;
import io.th0rgal.oraxen.config.SoundManager;
import io.th0rgal.oraxen.utils.LU;
import io.th0rgal.oraxen.utils.NoticeUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.actions.ClickActionManager;
import io.th0rgal.oraxen.utils.armorequipevent.ArmorEquipEvent;
import io.th0rgal.oraxen.utils.breaker.BreakerManager;
import io.th0rgal.oraxen.utils.breaker.LegacyBreakerManager;
import io.th0rgal.oraxen.utils.breaker.ModernBreakerManager;
import io.th0rgal.oraxen.utils.customarmor.CustomArmorListener;
import io.th0rgal.oraxen.utils.inventories.InvManager;
import io.th0rgal.protectionlib.ProtectionLib;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
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
    private SoundManager soundManager;
    private InvManager invManager;
    private PackGenerator packGenerator;
    @Nullable private OraxenPackServer packServer;
    private ClickActionManager clickActionManager;
    private BreakerManager breakerManager;

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
        fontManager = new FontManager(configsManager);
        soundManager = new SoundManager(configsManager.getSounds());
        breakerManager = VersionUtil.atOrAbove("1.20.5") ? new ModernBreakerManager(new ConcurrentHashMap<>())
                : new LegacyBreakerManager(new ConcurrentHashMap<>());
        ProtectionLib.setDebug(Settings.DEBUG.toBool());

        if (Settings.KEEP_UP_TO_DATE.toBool())
            new SettingsUpdater().handleSettingsUpdate();
        Bukkit.getPluginManager().registerEvents(new CustomArmorListener(), this);
        NMSHandlers.setupHandler();
        packGenerator = new PackGenerator();

        fontManager.registerEvents();
        Bukkit.getPluginManager().registerEvents(new ItemUpdater(), this);

        invManager = new InvManager();
        ArmorEquipEvent.registerListener(this);
        CustomBlockData.registerListener(this);

        new CommandsManager().loadCommands();

        packServer = OraxenPackServer.initializeServer();
        packServer.start();

        postLoading();
        CompatibilitiesManager.enableNativeCompatibilities();
        if (VersionUtil.isCompiled()) NoticeUtils.compileNotice();
        if (VersionUtil.isLeaked()) NoticeUtils.leakNotice();
    }

    private void postLoading() {
        new Metrics(this, 5371);
        new LU().l();
        Bukkit.getScheduler().runTask(this, () -> {
            MechanicsManager.registerNativeMechanics();
            OraxenItems.loadItems();
            RecipesManager.load(OraxenPlugin.get());
            packGenerator.generatePack();
        });
    }

    @Override
    public void onDisable() {
        if (packServer != null) packServer.stop();
        HandlerList.unregisterAll(this);
        FurnitureFactory.unregisterEvolution();
        FurnitureFactory.removeAllFurniturePackets();

        CompatibilitiesManager.disableCompatibilities();
        CommandAPI.onDisable();
        Message.PLUGIN_UNLOADED.log();
    }

    public Path packPath() {
        return getDataFolder().toPath().resolve("pack");
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
        PackGenerator.stopPackGeneration();
        this.packGenerator = packGenerator;
    }

    public OraxenPackServer packServer() {
        return packServer != null ? packServer : new EmptyServer();
    }
    public void packServer(@Nullable OraxenPackServer server) {
        if (packServer != null) packServer.stop();
        packServer = server;
        if (packServer != null) packServer.start();
    }

    public ClickActionManager clickActionManager() {
        return clickActionManager;
    }
}
