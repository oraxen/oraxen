package io.th0rgal.oraxen;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIConfig;
import io.th0rgal.oraxen.commands.CommandsManager;
import io.th0rgal.oraxen.compatibilities.CompatibilitiesManager;
import io.th0rgal.oraxen.config.ConfigsManager;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.pack.generation.ResourcePack;
import io.th0rgal.oraxen.pack.upload.UploadManager;
import io.th0rgal.oraxen.recipes.RecipesManager;
import io.th0rgal.oraxen.sound.SoundManager;
import io.th0rgal.oraxen.utils.OS;
import io.th0rgal.oraxen.utils.actions.ClickActionManager;
import io.th0rgal.oraxen.utils.armorequipevent.ArmorListener;
import io.th0rgal.oraxen.utils.breaker.BreakerSystem;
import io.th0rgal.oraxen.utils.inventories.InvManager;
import io.th0rgal.oraxen.utils.logs.Logs;
import io.th0rgal.protectionlib.ProtectionLib;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.Template;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class OraxenPlugin extends JavaPlugin {

    private static OraxenPlugin oraxen;
    private ConfigsManager configsManager;
    private BukkitAudiences audience;
    private UploadManager uploadManager;
    private FontManager fontManager;
    private SoundManager soundManager;
    private InvManager invManager;
    private ResourcePack resourcePack;
    private ClickActionManager clickActionManager;

    public OraxenPlugin() throws NoSuchFieldException, IllegalAccessException {
        oraxen = this;
        Logs.enableFilter();
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
        audience = BukkitAudiences.create(this);
        clickActionManager = new ClickActionManager(this);
        reloadConfigs();
        final PluginManager pluginManager = Bukkit.getPluginManager();
        resourcePack = new ResourcePack(this);
        MechanicsManager.registerNativeMechanics();
        //CustomBlockData.registerListener(this); //Handle this manually
        fontManager = new FontManager(configsManager);
        soundManager = new SoundManager(configsManager.getSound());
        OraxenItems.loadItems(configsManager);
        fontManager.registerEvents();
        pluginManager.registerEvents(new ItemUpdater(), this);
        resourcePack.generate(fontManager, soundManager);
        RecipesManager.load(this);
        invManager = new InvManager();
        new ArmorListener(Settings.ARMOR_EQUIP_EVENT_BYPASS.toStringList()).registerEvents(this);
        new BreakerSystem().registerListener();
        new CommandsManager().loadCommands();
        postLoading(configsManager);
        Message.PLUGIN_LOADED.log(Template.template("os", OS.getOs().getPlatformName()));
        CompatibilitiesManager.enableNativeCompatibilities();
    }

    private void postLoading(final ConfigsManager configsManager) {
        uploadManager = new UploadManager(this);
        uploadManager.uploadAsyncAndSendToPlayers(resourcePack);
        new Metrics(this, 5371);
        Bukkit.getScheduler().runTask(this, () -> OraxenItems.loadItems(configsManager));
    }

    @Override
    public void onDisable() {
        unregisterListeners();
        CompatibilitiesManager.disableCompatibilities();
        Message.PLUGIN_UNLOADED.log();
    }

    private void unregisterListeners() {
        fontManager.unregisterEvents();
        MechanicsManager.unloadListeners();
        HandlerList.unregisterAll(this);
    }

    public BukkitAudiences getAudience() {
        return audience;
    }

    public ConfigsManager reloadConfigs() {
        configsManager = new ConfigsManager(this);
        if (!configsManager.validatesConfig()) {
            Logs.logError("unable to validate config");
            getServer().getPluginManager().disablePlugin(this);
        }
        return configsManager;
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
