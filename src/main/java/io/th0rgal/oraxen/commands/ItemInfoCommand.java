package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.TextArgument;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenItems;
import org.bukkit.ChatColor;

import java.util.Map;

public class ItemInfoCommand {

    public CommandAPICommand getItemInfoCommand() {

        return new CommandAPICommand("iteminfo")
                .withPermission("oraxen.command.iteminfo")
                .withArguments(
                        new TextArgument("itemid").replaceSuggestions(ArgumentSuggestions.strings(OraxenItems.getItemNames()))
                )
                .executes((commandSender, args) -> {
                    String argument= (String) args[0];
                    if(argument.equals("all")){
                        for(Map.Entry<String, ItemBuilder> ib:OraxenItems.getEntries()){
                            commandSender.sendMessage(ChatColor.DARK_AQUA+"Oraxen id: "+ib.getKey()+", Material: "+ib.getValue().getReferenceClone().getType()+", "+ChatColor.DARK_GREEN+"CustomModelData: "+ib.getValue().getOraxenMeta().getCustomModelData());
                        }
                    }else{
                        ItemBuilder ib = OraxenItems.getItemById(argument);
                        if(ib==null){
                            commandSender.sendMessage(ChatColor.RED+"Item not found");
                            return;
                        }

                        commandSender.sendMessage(ChatColor.DARK_AQUA+"CustomModelData: "+ib.getOraxenMeta().getCustomModelData());
                        commandSender.sendMessage(ChatColor.DARK_GREEN+"Material: "+ib.getReferenceClone().getType());
                        commandSender.sendMessage(ChatColor.DARK_GREEN+"Model Name: "+ib.getOraxenMeta().getModelName());
                    }
                });
    }
}
