package io.th0rgal.oraxen.command.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import com.google.gson.JsonObject;
import com.oraxen.chimerate.commons.command.tree.nodes.Literal;
import com.oraxen.chimerate.commons.command.tree.nodes.Literal.Builder;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.command.CommandInfo;
import io.th0rgal.oraxen.command.permission.OraxenPermission;
import io.th0rgal.oraxen.utils.OS;
import io.th0rgal.oraxen.utils.reflection.ReflectionProvider;

public class Debug {

    public static CommandInfo build() {
        return new CommandInfo("debug", info -> {
            Builder<CommandSender> builder = Literal.of(info.getName());

            builder.executes((sender, context) -> {

                if (!OraxenPermission.COMMAND_DEBUG.required(sender))
                    return;

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
                minecraftJson.addProperty("server", ReflectionProvider.ORAXEN.getServerVersion().toString());
                minecraftJson.addProperty("minecraft", ReflectionProvider.ORAXEN.getMinecraftVersion().toString());

                report.add("operating_system", operatingSystemJson);
                report.add("plugin", pluginJson);
                report.add("minecraft", minecraftJson);

                sender.sendMessage(report.toString());
            });

            return builder;
        });
    }

}
