package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.utils.Utils;
import net.kyori.adventure.audience.Audience;
import org.bukkit.ChatColor;

import java.util.Map;

public class ItemInfoCommand {

    public CommandAPICommand getItemInfoCommand() {
        String[] itemNames = OraxenItems.getItemNames();
        if (itemNames == null) itemNames = new String[0];
        return new CommandAPICommand("iteminfo")
                .withPermission("oraxen.command.iteminfo")
                .withArguments(new StringArgument("itemid").replaceSuggestions(ArgumentSuggestions.strings(itemNames)))
                .executes((commandSender, args) -> {
                    String argument = (String) args[0];
                    Audience audience = OraxenPlugin.get().getAudience().sender(commandSender);
                    if (argument.equals("all")) {
                        for (Map.Entry<String, ItemBuilder> entry : OraxenItems.getEntries()) {
                            sendItemInfo(audience, entry.getValue());
                            commandSender.sendMessage("\n");
                        }
                    } else {
                        ItemBuilder ib = OraxenItems.getItemById(argument);
                        if (ib == null)
                            commandSender.sendMessage(ChatColor.RED + "Item not found");
                        else sendItemInfo(audience, ib);
                    }
                });
    }

    private void sendItemInfo(Audience sender, ItemBuilder builder) {
        sender.sendMessage(Utils.MINI_MESSAGE.deserialize("<dark_aqua>CustomModelData: <aqua>" + builder.getOraxenMeta().getCustomModelData()));
        sender.sendMessage(Utils.MINI_MESSAGE.deserialize("<dark_green>Material: <green>" + builder.getReferenceClone().getType()));
        sender.sendMessage(Utils.MINI_MESSAGE.deserialize("<dark_green>Model Name: <green>" + builder.getOraxenMeta().getModelName()));
    }
}
