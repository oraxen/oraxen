package io.th0rgal.oraxen;

import io.th0rgal.oraxen.command.CommandProvider;
import io.th0rgal.oraxen.compatibilities.CompatibilitiesManager;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.language.Translations;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.pack.generation.ResourcePack;
import io.th0rgal.oraxen.pack.upload.UploadManager;
import io.th0rgal.oraxen.recipes.RecipesManager;
import io.th0rgal.oraxen.settings.ConfigsManager;
import io.th0rgal.oraxen.settings.MessageOld;
import io.th0rgal.oraxen.settings.Plugin;
import io.th0rgal.oraxen.utils.OS;
import io.th0rgal.oraxen.utils.armorequipevent.ArmorListener;
import io.th0rgal.oraxen.utils.fastinv.FastInvManager;
import io.th0rgal.oraxen.utils.logs.Logs;
import io.th0rgal.oraxen.utils.signinput.SignMenuFactory;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class OraxenPlugin extends JavaPlugin {

    private SignMenuFactory signMenuFactory;
    private UploadManager uploadManager;

    public OraxenPlugin() throws Exception {
        Logs.enableFilter();
    }

    private void postLoading(ResourcePack resourcePack, ConfigsManager configsManager) {
        CommandProvider.register();
        (this.uploadManager = new UploadManager(this)).uploadAsyncAndSendToPlayers(resourcePack);
        new Metrics(this, 5371);
        this.signMenuFactory = new SignMenuFactory(this);
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> OraxenItems.loadItems(configsManager));
    }

    public void onEnable() {
        ConfigsManager configsManager = new ConfigsManager(this);
        if (!configsManager.validatesConfig()) {
            MessageOld.CONFIGS_VALIDATION_FAILED.logError();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        Translations.MANAGER.reloadCatch();
        MechanicsManager.registerNativeMechanics();
        CompatibilitiesManager.enableNativeCompatibilities();
        OraxenItems.loadItems(configsManager);
        ResourcePack resourcePack = new ResourcePack(this);
        RecipesManager.load(this);
        FastInvManager.register(this);
        new ArmorListener(Plugin.ARMOR_EQUIP_EVENT_BYPASS.getAsStringList()).registerEvents(this);
        postLoading(resourcePack, configsManager);
        Logs.log(ChatColor.GREEN + "Successfully loaded on " + OS.getOs().getPlatformName());
    }

    public void onDisable() {
        MechanicsManager.unloadListeners();
        CompatibilitiesManager.disableCompatibilities();
        Logs.log(ChatColor.GREEN + "Successfully unloaded");
    }

    public static OraxenPlugin get() {
        return OraxenPlugin.getPlugin(OraxenPlugin.class);
    }

    public SignMenuFactory getSignMenuFactory() {
        return signMenuFactory;
    }

    public UploadManager getUploadManager() {
        return uploadManager;
    }

}
