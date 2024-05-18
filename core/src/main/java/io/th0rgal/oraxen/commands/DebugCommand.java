package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.ConfigsManager;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;

public class DebugCommand {

    CommandAPICommand getDebugCommand() {
        return new CommandAPICommand("debug")
                .withPermission("oraxen.command.debug")
                .withOptionalArguments(new StringArgument("toggle"))
                .executes((sender, args) -> {
                    ConfigsManager configsManager = OraxenPlugin.get().getConfigsManager();
                    YamlConfiguration settings = configsManager.getSettings();
                    boolean debugState = args.getOptional("toggle").isPresent() ? Boolean.parseBoolean(args.getOptional("toggle").get().toString()) : !settings.getBoolean("debug", true);
                    settings.set("debug", debugState);
                    try {
                        settings.save(configsManager.getSettingsFile());
                        String state = (debugState ? "enabled" : "disabled");
                        ProtectionLib.setDebug(debugState);
                        Message.DEBUG_TOGGLE.send(sender, AdventureUtils.tagResolver("state", state));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

}
