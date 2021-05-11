package io.th0rgal.oraxen.command.commands;

import static io.th0rgal.oraxen.command.argument.ArgumentHelper.*;

import io.th0rgal.oraxen.language.Message;
import io.th0rgal.oraxen.utils.general.Placeholder;
import org.bukkit.command.CommandSender;

import com.syntaxphoenix.syntaxapi.command.Arguments;
import com.syntaxphoenix.syntaxapi.command.DefaultCompletion;
import com.syntaxphoenix.syntaxapi.command.arguments.StringArgument;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.command.CommandInfo;
import io.th0rgal.oraxen.command.MinecraftInfo;
import io.th0rgal.oraxen.command.OraxenCommand;
import io.th0rgal.oraxen.command.argument.Reloadable;
import io.th0rgal.oraxen.command.condition.Conditions;
import io.th0rgal.oraxen.command.permission.OraxenPermission;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.language.Translations;
import io.th0rgal.oraxen.pack.generation.ResourcePack;
import io.th0rgal.oraxen.recipes.RecipesManager;

public class Reload extends OraxenCommand {

    public static final OraxenCommand COMMAND = new Reload();

    public static CommandInfo info() {
        return new CommandInfo("reload", COMMAND, "rl").setDescription("Reload Oraxen components");
    }

    private Reload() {
    }

    @Override
    public void execute(MinecraftInfo info, Arguments arguments) {
        CommandSender sender = info.getSender();

        if (Conditions.reqPerm(OraxenPermission.COMMAND_RELOAD).isFalse(sender))
            return;

        switch (get(arguments, 1, Reloadable::fromArgument).orElse(Reloadable.ALL)) {
            case ITEMS:
                reloadItems(sender);
                break;

            case PACK:
                reloadPack(OraxenPlugin.get(), sender);
                break;

            case RECIPES:
                RecipesManager.reload(OraxenPlugin.get());
                break;

            case MESSAGES:
                Translations.MANAGER.reloadCatch();
                break;

            default:
                OraxenPlugin oraxen = OraxenPlugin.get();
                Translations.MANAGER.reloadCatch();
                reloadItems(sender);
                reloadPack(oraxen, sender);
                RecipesManager.reload(oraxen);
                break;
        }
    }

    @Override
    public DefaultCompletion complete(MinecraftInfo info, Arguments arguments) {
        DefaultCompletion completion = new DefaultCompletion();

        if (Conditions.hasPerm(OraxenPermission.COMMAND_RELOAD).isFalse(info.getSender()))
            return completion;

        if (arguments.count() == 1) {
            Reloadable[] types = Reloadable.values();
            for (Reloadable type : types) completion.add(new StringArgument(type.name()));
        }
        return completion;
    }

    private static void reloadItems(CommandSender sender) {
        Message.RELOAD.send(sender, Placeholder.of("reloaded", "items"));
        OraxenItems.loadItems();
    }

    private static void reloadPack(OraxenPlugin plugin, CommandSender sender) {
        Message.PACK_REGENERATED.send(sender);
        ResourcePack resourcePack = new ResourcePack(plugin);
        plugin.getUploadManager().uploadAsyncAndSendToPlayers(resourcePack, true);
    }

}
