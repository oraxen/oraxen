package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.utils.AdventureUtils;

public class VersionCommand {

    CommandAPICommand getVersionCommand() {
        return new CommandAPICommand("version")
                .withPermission("oraxen.command.version")
                .executes((sender, args) -> {
                    Message.VERSION.send(sender, AdventureUtils.tagResolver("version", OraxenPlugin.get().getDescription().getVersion()));
                });
    }
}
