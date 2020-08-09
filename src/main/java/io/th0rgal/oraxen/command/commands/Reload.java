package io.th0rgal.oraxen.command.commands;

import org.bukkit.command.CommandSender;

import com.oraxen.chimerate.commons.command.tree.nodes.Argument;
import com.oraxen.chimerate.commons.command.tree.nodes.Literal;
import com.oraxen.chimerate.commons.command.tree.nodes.Literal.Builder;

import io.th0rgal.oraxen.Oraxen;
import io.th0rgal.oraxen.command.CommandInfo;
import io.th0rgal.oraxen.command.argument.Reloadable;
import io.th0rgal.oraxen.command.permission.OraxenPermission;
import io.th0rgal.oraxen.command.types.ReloadableType;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.pack.generation.ResourcePack;
import io.th0rgal.oraxen.recipes.RecipesManager;
import io.th0rgal.oraxen.settings.MessageOld;

public class Reload {

    public static CommandInfo build() {
        return new CommandInfo("reload", info -> {
            Builder<CommandSender> builder = Literal.of(info.getName()).alias(info.getAliases());

            builder.then(Argument.of("type", ReloadableType.TYPE).executes((sender, context) -> {

                if (!OraxenPermission.COMMAND_RELOAD.has(sender)) {
                    MessageOld.DONT_HAVE_PERMISSION.send(sender);
                    return;
                }

                switch (context.getArgument("type", Reloadable.class)) {
                case ITEMS:
                    reloadItems(sender);
                    break;

                case PACK:
                    reloadPack(Oraxen.get(), sender);
                    break;

                case RECIPES:
                    RecipesManager.reload(Oraxen.get());
                    break;

                default:
                    Oraxen oraxen = Oraxen.get();
                    reloadItems(sender);
                    reloadPack(oraxen, sender);
                    RecipesManager.reload(oraxen);
                    break;
                }
            })).executes((sender, context) -> {

                if (!OraxenPermission.COMMAND_RELOAD.has(sender)) {
                    MessageOld.DONT_HAVE_PERMISSION.send(sender);
                    return;
                }
                
                Oraxen oraxen = Oraxen.get();
                reloadItems(sender);
                reloadPack(oraxen, sender);
                RecipesManager.reload(oraxen);
            });

            return builder;
        }, "rl");
    }

    private static void reloadItems(CommandSender sender) {
        MessageOld.RELOAD.send(sender, "items");
        OraxenItems.loadItems();
    }

    private static void reloadPack(Oraxen plugin, CommandSender sender) {
        MessageOld.REGENERATED.send(sender, "resourcepack");
        ResourcePack resourcePack = new ResourcePack(plugin);
        plugin.getUploadManager().uploadAsyncAndSendToPlayers(resourcePack);
    }

}
