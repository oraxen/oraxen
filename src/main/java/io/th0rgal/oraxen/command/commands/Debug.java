package io.th0rgal.oraxen.command.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import com.google.gson.JsonObject;
import com.syntaxphoenix.syntaxapi.command.Arguments;
import com.syntaxphoenix.syntaxapi.command.DefaultCompletion;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.command.CommandInfo;
import io.th0rgal.oraxen.command.MinecraftInfo;
import io.th0rgal.oraxen.command.OraxenCommand;
import io.th0rgal.oraxen.command.permission.OraxenPermission;
import io.th0rgal.oraxen.utils.OS;

public final class Debug extends OraxenCommand {

    public static final OraxenCommand COMMAND = new Debug();

    public static CommandInfo info() {
        return new CommandInfo("debug", COMMAND).setDescription("Just a debug command");
    }

    private Debug() {
    }

    @Override
    public void execute(MinecraftInfo info, Arguments arguments) {

        CommandSender sender = info.getSender();

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

        report.add("operating_system", operatingSystemJson);
        report.add("plugin", pluginJson);
        report.add("minecraft", minecraftJson);

        sender.sendMessage(report.toString());

    }

    @Override
    public DefaultCompletion complete(MinecraftInfo info, Arguments arguments) {
        return new DefaultCompletion();
    }

}
