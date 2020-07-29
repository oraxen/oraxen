package io.th0rgal.oraxen.deprecated.commands.subcommands;

import io.th0rgal.oraxen.Oraxen;
import io.th0rgal.oraxen.deprecated.commands.CommandInterface;
import io.th0rgal.oraxen.deprecated.commands.brigadier.BrigadierManager;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.pack.generation.ResourcePack;
import io.th0rgal.oraxen.pack.upload.UploadManager;
import io.th0rgal.oraxen.recipes.RecipesManager;
import io.th0rgal.oraxen.settings.MessageOld;
import me.lucko.commodore.CommodoreProvider;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class Reload implements CommandInterface {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!sender.hasPermission("oraxen.command.reload")) {
            MessageOld.DONT_HAVE_PERMISSION.send(sender, "oraxen.command.reload");
            return true;
        }
        JavaPlugin plugin = Oraxen.get();

        if (args.length <= 1) {
            reloadItems(sender);
            reloadPack(plugin, sender);
            RecipesManager.reload(plugin);
            return true;
        }

        switch (args[1]) {
            case "items":
                reloadItems(sender);
                break;

            case "pack":
                reloadPack(plugin, sender);
                break;

            case "recipes":
                RecipesManager.reload(plugin);
                break;

            default:
                break;
        }

        return false;
    }

    private void reloadItems(CommandSender sender) {
        MessageOld.RELOAD.send(sender, "items");
        OraxenItems.loadItems();
        if(CommodoreProvider.isSupported())
            BrigadierManager.registerCompletions(CommodoreProvider.getCommodore(Oraxen.get()), Oraxen.get().getCommand("oraxen"));
    }

    private void reloadPack(JavaPlugin plugin, CommandSender sender) {
        MessageOld.REGENERATED.send(sender, "resourcepack");
        ResourcePack resourcePack = new ResourcePack(plugin);
        new UploadManager(plugin).uploadAsyncAndSendToPlayers(resourcePack);
    }

}

