package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import io.th0rgal.oraxen.OraxenPlugin;

public class VersionCommand {

    CommandAPICommand getVersionCommand() {
        return new CommandAPICommand("version")
                .withPermission("oraxen.command.version")
                .executes((sender, args) -> {
                    sender.sendMessage("Oraxen version: " + OraxenPlugin.get().getDescription().getVersion());
                });
    }
}
