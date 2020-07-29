package io.th0rgal.oraxen.deprecated.commands.subcommands;

import com.google.gson.JsonObject;
import io.th0rgal.oraxen.Oraxen;
import io.th0rgal.oraxen.deprecated.commands.CommandInterface;
import io.th0rgal.oraxen.settings.MessageOld;

import io.th0rgal.oraxen.utils.OS;
import io.th0rgal.oraxen.utils.reflection.ReflectionProvider;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class Debug implements CommandInterface {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!sender.hasPermission("oraxen.command.debug")) {
            MessageOld.DONT_HAVE_PERMISSION.send(sender, "oraxen.command.debug");
            return false;
        }

        JsonObject report = new JsonObject();
        OS system = OS.getOs();

        JsonObject operatingSystemJson = new JsonObject();
        operatingSystemJson.addProperty("name", system.getName());
        operatingSystemJson.addProperty("version", system.getVersion());
        operatingSystemJson.addProperty("platform_name", system.getPlatformName());
        operatingSystemJson.addProperty("arch", system.getArch());

        JsonObject pluginJson = new JsonObject();
        pluginJson.addProperty("version", Oraxen.get().getDescription().getVersion());
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

        return true;
    }

}

