package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public class ModelDataCommand {
    CommandAPICommand getHighestModelDataCommand() {
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
                    Component report = Component.empty();
                    for (Map.Entry<Material, Integer> entry : itemMap.entrySet()) {
                        String message = (ChatColor.DARK_AQUA + entry.getKey().name() + ": " + ChatColor.DARK_GREEN + entry.getValue().toString() + "\n");
                        report = report.append(Component.text(message));
                    }
                    OraxenPlugin.get().getAudience().sender(sender).sendMessage(report);
                });
    }
}
