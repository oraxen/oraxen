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

import java.io.File;

public class Reload implements CommandInterface {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!sender.hasPermission("oraxen.command.reload")) {
            Message.DONT_HAVE_PERMISSION.send(sender, "oraxen.command.reload");
            return false;
        }
        JavaPlugin plugin = OraxenPlugin.get();

        switch (args[1]) {
            case "items":
                Message.RELOAD.send(sender, "items");
                OraxenItems.loadItems(plugin);
                break;

            case "pack":
                Message.REGENERATED.send(sender, "resourcepack");
                File resourcePack = ResourcePack.generate(plugin);
                new UploadManager(plugin).uploadAsyncAndSendToPlayers(resourcePack);
                break;

            default:
                break;
        }

        return false;
    }
}