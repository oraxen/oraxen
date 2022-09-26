package io.th0rgal.oraxen.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.jorel.commandapi.CommandAPICommand;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.OS;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

import java.util.Map;

public class DebugCommand {

    public CommandAPICommand getDebugCommand() {
        return new CommandAPICommand("debug")
                .withPermission("oraxen.command.debug")
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

                    Component msg = Component.empty();
                    for (Map.Entry<String, JsonElement> entry : report.entrySet()) {
                        msg = msg.append(Component.text(entry.getKey())).append(Component.text(entry.getValue().toString()));
                    }
                    OraxenPlugin.get().getAudience().sender(sender).sendMessage(msg);
                });
    }

}
