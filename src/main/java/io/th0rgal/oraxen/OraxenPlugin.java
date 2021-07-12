package io.th0rgal.oraxen;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIConfig;
import io.th0rgal.oraxen.commands.CommandsManager;
import io.th0rgal.oraxen.compatibilities.CompatibilitiesManager;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.pack.generation.ResourcePack;
import io.th0rgal.oraxen.pack.upload.UploadManager;
import io.th0rgal.oraxen.recipes.RecipesManager;
import io.th0rgal.oraxen.config.ConfigsManager;
import io.th0rgal.oraxen.utils.metrics.Metrics;
import io.th0rgal.oraxen.utils.OS;
import io.th0rgal.oraxen.utils.armorequipevent.ArmorListener;
import io.th0rgal.oraxen.utils.fastinv.FastInvManager;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class OraxenPlugin extends JavaPlugin {

    private UploadManager uploadManager;
    private static OraxenPlugin oraxen;
    private YamlConfiguration settings;
    private YamlConfiguration language;
    private BukkitAudiences audience;

    public OraxenPlugin() throws Exception {
        oraxen = this;
        Logs.enableFilter();
    }

    private void postLoading(ResourcePack resourcePack, ConfigsManager configsManager) {
        (this.uploadManager = new UploadManager(this)).uploadAsyncAndSendToPlayers(resourcePack);
        new Metrics(this, 5371);
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> OraxenItems.loadItems(configsManager));
    }

    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIConfig().silentLogs(true));
    }

    public void onEnable() {
        CommandAPI.onEnable(this);
        ConfigsManager configsManager = new ConfigsManager(this);
        if (!configsManager.validatesConfig()) {
            Logs.logError("unable to validate config");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        settings = configsManager.getSettings();
        language = configsManager.getLanguage();

        new CommandsManager().loadCommands();
        audience = BukkitAudiences.create(this);
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(configsManager, this);
        MechanicsManager.registerNativeMechanics();
        CompatibilitiesManager.enableNativeCompatibilities();
        OraxenItems.loadItems(configsManager);
        //MechanicsManager.unloadListeners(); // we need to avoid double loading
        ResourcePack resourcePack = new ResourcePack(this);
        RecipesManager.load(this);
        FastInvManager.register(this);
        new ArmorListener(Settings.ARMOR_EQUIP_EVENT_BYPASS.toStringList()).registerEvents(this);
        postLoading(resourcePack, configsManager);
        Logs.log(ChatColor.GREEN + "Successfully loaded on " + OS.getOs().getPlatformName());
    }

    public void onDisable() {
        unregisterListeners();
        CompatibilitiesManager.disableCompatibilities();
        Logs.log(ChatColor.GREEN + "Successfully unloaded");
    }

    private void unregisterListeners() {
        MechanicsManager.unloadListeners();
        HandlerList.unregisterAll(this);
    }

    public static OraxenPlugin get() {
        return oraxen;
    }

    public static boolean getProtocolLib() {
        return Bukkit.getPluginManager().getPlugin("ProtocolLib") != null;
    }

    public UploadManager getUploadManager() {
        return uploadManager;
    }

    public YamlConfiguration getSettings() {
        return settings;
    }

    public YamlConfiguration getLanguage() {
        return language;
    }

    public BukkitAudiences getAudience() {
        return audience;
    }

}
