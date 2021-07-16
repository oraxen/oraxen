package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.pack.generation.ResourcePack;
import io.th0rgal.oraxen.recipes.RecipesManager;
import org.bukkit.command.CommandSender;

public class ReloadCommand {

    public CommandAPICommand getReloadCommand() {
        return new CommandAPICommand("reload")
                .withAliases("rl")
                .withPermission("oraxen.command.reload")
                .withArguments(new StringArgument("type").replaceSuggestions(info ->
                        new String[]{"items", "pack", "recipes", "messages", "all"}))
                .executes((sender, args) -> {
                    switch (((String) args[0]).toUpperCase()) {
                        case "ITEMS":
                            reloadItems(sender);
                            break;

                        case "PACK":
                            reloadPack(OraxenPlugin.get(), sender);
                            break;

                        case "RECIPES":
                            RecipesManager.reload(OraxenPlugin.get());
                            break;

                        case "CONFIGS":
                            OraxenPlugin.get().reloadConfigs();
                            break;

                        default:
                            OraxenPlugin oraxen = OraxenPlugin.get();
                            OraxenPlugin.get().reloadConfigs();
                            reloadItems(sender);
                            reloadPack(oraxen, sender);
                            RecipesManager.reload(oraxen);
                            break;
                    }
                });
    }

    private static void reloadItems(CommandSender sender) {
        Message.RELOAD.send(sender, "reloaded", "items");
        OraxenItems.loadItems();
    }

    private static void reloadPack(OraxenPlugin plugin, CommandSender sender) {
        Message.PACK_REGENERATED.send(sender);
        ResourcePack resourcePack = new ResourcePack(plugin);
        plugin.getUploadManager().uploadAsyncAndSendToPlayers(resourcePack, true);
    }

}
