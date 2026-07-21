package io.th0rgal.oraxen.commands;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.Message;
import io.th0rgal.oraxen.utils.AdventureUtils;

public class VersionCommand {

    OraxenCommand getVersionCommand() {
        return new OraxenCommand("version")
                .withPermission("oraxen.command.version")
                .executes((sender, args) -> {
                    Message.VERSION.send(sender, AdventureUtils.tagResolver("version", OraxenPlugin.get().getDescription().getVersion()));
                });
    }
}
