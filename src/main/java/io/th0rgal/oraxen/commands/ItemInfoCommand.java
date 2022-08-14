package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenItems;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Map;

public class ItemInfoCommand {

    public CommandAPICommand getItemInfoCommand() {

        return new CommandAPICommand("iteminfo")
                .withPermission("oraxen.command.iteminfo")
                .withArguments(new StringArgument("itemid").replaceSuggestions(ArgumentSuggestions.strings(OraxenItems.getItemNames())))
                .executes((commandSender, args) -> {
                    String argument = (String) args[0];
                    if (argument.equals("all")) {
                        for (Map.Entry<String, ItemBuilder> entry : OraxenItems.getEntries()) {
                            sendItemInfo(commandSender, entry.getValue());
                            commandSender.sendMessage("\n");
                        }
                    } else {
                        ItemBuilder ib = OraxenItems.getItemById(argument);
                        if (ib == null)
                            commandSender.sendMessage(ChatColor.RED + "Item not found");
                        else sendItemInfo(commandSender, ib);
                    }
                });
    }

    private void sendItemInfo(CommandSender sender, ItemBuilder builder) {
        sender.sendMessage(ChatColor.DARK_AQUA + "CustomModelData: " + builder.getOraxenMeta().getCustomModelData());
        sender.sendMessage(ChatColor.DARK_GREEN + "Material: " + builder.getReferenceClone().getType());
        sender.sendMessage(ChatColor.DARK_GREEN + "Model Name: " + builder.getOraxenMeta().getModelName());
    }
}
