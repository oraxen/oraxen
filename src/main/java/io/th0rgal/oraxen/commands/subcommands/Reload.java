package io.th0rgal.oraxen.commands.subcommands;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.commands.CommandInterface;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.pack.generation.ResourcePack;
import io.th0rgal.oraxen.pack.upload.UploadManager;
import io.th0rgal.oraxen.settings.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class Reload implements CommandInterface {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!sender.hasPermission("oraxen.command.reload")) {
            Message.DONT_HAVE_PERMISSION.send(sender, "oraxen.command.reload");
            return false;
        }
        JavaPlugin plugin = OraxenPlugin.get();

        if (args.length <= 1) {
            reloadItems(plugin, sender);
            reloadPack(plugin, sender);
        }

        switch (args[1]) {
            case "items":
                reloadItems(plugin, sender);
                break;

            case "pack":
                reloadPack(plugin, sender);
                break;

            default:
                break;
        }

        return false;
    }

    private void reloadItems(JavaPlugin plugin, CommandSender sender) {
        Message.RELOAD.send(sender, "items");
        OraxenItems.loadItems(plugin);
    }

    private void reloadPack(JavaPlugin plugin, CommandSender sender) {
        Message.REGENERATED.send(sender, "resourcepack");
        ResourcePack resourcePack = new ResourcePack(plugin);
        new UploadManager(plugin).uploadAsyncAndSendToPlayers(resourcePack);
    }

}

