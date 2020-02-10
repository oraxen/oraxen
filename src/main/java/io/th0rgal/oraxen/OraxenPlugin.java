package io.th0rgal.oraxen;

import io.th0rgal.oraxen.commands.BaseCommand;
import io.th0rgal.oraxen.commands.CommandHandler;
import io.th0rgal.oraxen.commands.brigadier.BrigadierManager;
import io.th0rgal.oraxen.commands.subcommands.*;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.pack.upload.UploadManager;
import io.th0rgal.oraxen.recipes.RecipesManager;
import io.th0rgal.oraxen.settings.ConfigsValidator;
import io.th0rgal.oraxen.settings.Plugin;
import io.th0rgal.oraxen.utils.OS;
import io.th0rgal.oraxen.utils.armorequipevent.ArmorListener;
import io.th0rgal.oraxen.utils.logs.Logs;
import io.th0rgal.oraxen.pack.generation.ResourcePack;

import io.th0rgal.oraxen.utils.fastinv.FastInvManager;
import me.lucko.commodore.CommodoreProvider;
import org.bstats.bukkit.Metrics;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class OraxenPlugin extends JavaPlugin {

    private static OraxenPlugin instance;

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
                .register("give", new Give());
        PluginCommand command = getCommand("oraxen");
        command.setExecutor(handler);
        // use brigadier if supported
        if (CommodoreProvider.isSupported())
            BrigadierManager.registerCompletions(CommodoreProvider.getCommodore(this), command);
    }

    public void onEnable() {
        new ConfigsValidator(this).validatesConfig();
        MechanicsManager.registerNativeMechanics();
        OraxenItems.loadItems(this);
        ResourcePack resourcePack = new ResourcePack(this);
        RecipesManager.load(this);
        FastInvManager.register(this);
        new ArmorListener(Plugin.ARMOR_EQUIP_EVENT_BYPASS.getAsStringList()).registerEvents(this);
        registerCommands();
        Logs.log(ChatColor.GREEN + "Successfully loaded on " + OS.getOs().getPlatformName());
        new UploadManager(this).uploadAsyncAndSendToPlayers(resourcePack);
        new Metrics(this, 5371);
    }

    public void onDisable() {
        MechanicsManager.unloadListeners();

        Logs.log(ChatColor.GREEN + "Successfully unloaded");
    }

    public static OraxenPlugin get() {
        return instance;
    }

}
