package io.th0rgal.oraxen.commands;

import com.google.gson.JsonObject;
import dev.jorel.commandapi.CommandAPICommand;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.utils.OS;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public class DebugCommand {

    public CommandAPICommand getDebugCommand() {
        return new CommandAPICommand("debug")
                .withPermission("oraxen.command.debug")
                .withSubcommand(getHighestModelDataCommand())
                .executes((sender, args) -> {

                    JsonObject report = new JsonObject();
                    OS system = OS.getOs();

                    JsonObject operatingSystemJson = new JsonObject();
                    operatingSystemJson.addProperty("name", system.getName());
                    operatingSystemJson.addProperty("version", system.getVersion());
                    operatingSystemJson.addProperty("platform_name", system.getPlatformName());
                    operatingSystemJson.addProperty("arch", system.getArch());

                    JsonObject pluginJson = new JsonObject();
                    pluginJson.addProperty("version", OraxenPlugin.get().getDescription().getVersion());
                    pluginJson.addProperty("user", "%%__USER__%%");
                    pluginJson.addProperty("resource", "%%__RESOURCE__%%");
                    pluginJson.addProperty("nonce", "%%__NONCE__%%");

                    JsonObject minecraftJson = new JsonObject();
                    minecraftJson.addProperty("name", Bukkit.getVersion());
                    report.add("operating_system", operatingSystemJson);
                    report.add("plugin", pluginJson);
                    report.add("minecraft", minecraftJson);

                    sender.sendMessage(report.toString());
                });
    }

    private CommandAPICommand getHighestModelDataCommand() {
        JsonObject report = new JsonObject();
        return new CommandAPICommand("highest_modeldata")
                .withAliases("h_md")
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
                    for (Map.Entry<Material, Integer> entry : itemMap.entrySet()) {
                        report.addProperty(entry.getKey().name(), entry.getValue());
                    }
                    sender.sendMessage(report.toString());
                });
    }

}
