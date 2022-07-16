package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenItems;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public class ModelDataCommand {
    public CommandAPICommand getHighestModelDataCommand() {
        return new CommandAPICommand("highest_modeldata")
                .withAliases("h_md")
                .withPermission("oraxen.command.debug")
                .executes((sender, args) -> {
                    Map<Material, Integer> itemMap = new HashMap<>();
                    for (ItemBuilder builder : OraxenItems.getItems()) {
                        int currentModelData = builder.getOraxenMeta().getCustomModelData();
                        Material type = builder.build().getType();

                        if (currentModelData != 0) itemMap.putIfAbsent(type, currentModelData);
                        if (itemMap.containsKey(type) && itemMap.get(type) < currentModelData) {
                            itemMap.put(type, currentModelData);
                        }
                    }
                    BaseComponent[] report = new ComponentBuilder("").create();
                    for (Map.Entry<Material, Integer> entry : itemMap.entrySet()) {
                        String message = (ChatColor.DARK_AQUA + entry.getKey().name() + ": " + ChatColor.DARK_GREEN + entry.getValue().toString() + "\n");
                        report = new ComponentBuilder(message).append(report.clone()).create();
                    }
                    sender.spigot().sendMessage(report);
                });
    }
}
