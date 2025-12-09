package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import io.th0rgal.oraxen.utils.logs.Logs;
import io.th0rgal.oraxen.utils.schema.SchemaGenerator;

public class SchemaCommand {

    public CommandAPICommand getSchemaCommand() {
        return new CommandAPICommand("schema")
                .withPermission("oraxen.command.schema")
                .executes((sender, args) -> {
                    Logs.logInfo("Generating Oraxen schema...");
                    SchemaGenerator.generateAndSave();
                    sender.sendMessage("Schema generated! Check plugins/Oraxen/oraxen-schema.json");
                });
    }
}
