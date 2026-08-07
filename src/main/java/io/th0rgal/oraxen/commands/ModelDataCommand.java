package io.th0rgal.oraxen.commands;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.utils.AdventureUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public class ModelDataCommand {
    OraxenCommand getHighestModelDataCommand() {
        return new OraxenCommand("highest-modeldata")
                .withPermission("oraxen.command.debug")
                .executes((sender, args) -> {
                    Map<Material, Integer> itemMap = new HashMap<>();
                    for (ItemBuilder builder : OraxenItems.getItems()) {
                        Integer currentModelData = builder.getOraxenMeta().getCustomModelData();
                        if (currentModelData == null || currentModelData == 0) {
                            continue;
                        }
                        Material type = builder.build().getType();

                        itemMap.putIfAbsent(type, currentModelData);
                        if (itemMap.containsKey(type) && itemMap.get(type) < currentModelData) {
                            itemMap.put(type, currentModelData);
                        }
                    }
                    Component report = Component.empty();
                    for (Map.Entry<Material, Integer> entry : itemMap.entrySet()) {
                        String message = (ChatColor.DARK_AQUA + entry.getKey().name() + ": " + ChatColor.DARK_GREEN + entry.getValue().toString() + "\n");
                        report = report.append(Component.text(message));
                    }
                    AdventureUtils.sendMessage(sender, report);
                });
    }
}
