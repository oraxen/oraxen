package io.th0rgal.oraxen;

import io.th0rgal.oraxen.commands.BaseCommand;
import io.th0rgal.oraxen.commands.CommandHandler;
import io.th0rgal.oraxen.commands.brigadier.BrigadierManager;
import io.th0rgal.oraxen.commands.subcommands.Debug;
import io.th0rgal.oraxen.commands.subcommands.Give;
import io.th0rgal.oraxen.commands.subcommands.InventoryVisualizer;
import io.th0rgal.oraxen.commands.subcommands.Recipes;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.listeners.EventsManager;
import io.th0rgal.oraxen.recipes.RecipesManager;
import io.th0rgal.oraxen.utils.Logs;
import io.th0rgal.oraxen.pack.ResourcePack;

import io.th0rgal.oraxen.utils.fastinv.FastInvManager;
import me.lucko.commodore.CommodoreProvider;
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
        MechanicsManager.registerNativeMechanics();
        OraxenItems.loadItems(this);
        ResourcePack.generate(this);
        RecipesManager.load(this);
        FastInvManager.register(this);
        registerCommands();
        Logs.log(ChatColor.GREEN + "Successfully loaded");
        new EventsManager(this).registerNativeEvents();
    }

    public void onDisable() {
        MechanicsManager.unloadListeners();

        Logs.log(ChatColor.GREEN + "Successfully unloaded");
    }

    public static OraxenPlugin get() {
        return instance;
    }

}
