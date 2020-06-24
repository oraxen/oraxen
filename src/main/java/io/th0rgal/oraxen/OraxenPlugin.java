package io.th0rgal.oraxen;

import io.th0rgal.oraxen.commands.BaseCommand;
import io.th0rgal.oraxen.commands.CommandHandler;
import io.th0rgal.oraxen.commands.brigadier.BrigadierManager;
import io.th0rgal.oraxen.commands.subcommands.*;
import io.th0rgal.oraxen.compatibility.mythicmobs.MythicMobsListener;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.pack.generation.ResourcePack;
import io.th0rgal.oraxen.pack.upload.UploadManager;
import io.th0rgal.oraxen.recipes.RecipesManager;
import io.th0rgal.oraxen.settings.ConfigsManager;
import io.th0rgal.oraxen.settings.Message;
import io.th0rgal.oraxen.settings.Plugin;
import io.th0rgal.oraxen.utils.OS;
import io.th0rgal.oraxen.utils.armorequipevent.ArmorListener;
import io.th0rgal.oraxen.utils.fastinv.FastInvManager;
import io.th0rgal.oraxen.utils.logs.Logs;
import io.th0rgal.oraxen.utils.signinput.SignMenuFactory;
import me.lucko.commodore.CommodoreProvider;
import org.bstats.bukkit.Metrics;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class OraxenPlugin extends JavaPlugin {

    private static OraxenPlugin instance;
    private SignMenuFactory signMenuFactory;


    public OraxenPlugin() throws Exception {
        instance = this;
        Logs.enableFilter();
    }

    private void registerCommands() {
        CommandHandler handler = new CommandHandler()
                .register("oraxen", new BaseCommand())
                .register("debug", new Debug())
                .register("reload", new Reload())
                .register("pack", new Pack())
                .register("recipes", new Recipes())
                .register("inv", new InventoryVisualizer())
                .register("give", new Give())
                .register("repair", new Repair());
        PluginCommand command = getCommand("oraxen");
        assert command != null;
        command.setExecutor(handler);
        // use brigadier if supported
        if (CommodoreProvider.isSupported())
            BrigadierManager.registerCompletions(CommodoreProvider.getCommodore(this), command);
    }

    public void onEnable() {
        ConfigsManager configsManager = new ConfigsManager(this);
        if (!configsManager.validatesConfig()) {
            Message.CONFIGS_VALIDATION_FAILED.logError();
            getServer().getPluginManager().disablePlugin(this);
        }
        MechanicsManager.registerNativeMechanics();
        OraxenItems.loadItems(configsManager);
        ResourcePack resourcePack = new ResourcePack(this);
        RecipesManager.load(this);
        FastInvManager.register(this);
        new ArmorListener(Plugin.ARMOR_EQUIP_EVENT_BYPASS.getAsStringList()).registerEvents(this);
        if (getServer().getPluginManager().isPluginEnabled("MythicMobs"))
            new MythicMobsListener().registerEvents(this);
        registerCommands();
        Logs.log(ChatColor.GREEN + "Successfully loaded on " + OS.getOs().getPlatformName());
        new UploadManager(this).uploadAsyncAndSendToPlayers(resourcePack);
        new Metrics(this, 5371);
        this.signMenuFactory = new SignMenuFactory(this);
    }

    public void onDisable() {
        MechanicsManager.unloadListeners();
        Logs.log(ChatColor.GREEN + "Successfully unloaded");
    }

    public static OraxenPlugin get() {
        return instance;
    }

    public SignMenuFactory getSignMenuFactory() {
        return signMenuFactory;
    }

}
