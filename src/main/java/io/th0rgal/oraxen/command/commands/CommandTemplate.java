package io.th0rgal.oraxen.command.commands;

import com.syntaxphoenix.syntaxapi.command.Arguments;
import com.syntaxphoenix.syntaxapi.command.DefaultCompletion;

import io.th0rgal.oraxen.command.CommandInfo;
import io.th0rgal.oraxen.command.MinecraftInfo;
import io.th0rgal.oraxen.command.OraxenCommand;

public final class CommandTemplate extends OraxenCommand {

    public static final OraxenCommand COMMAND = new CommandTemplate();

    public static CommandInfo info() {
        return new CommandInfo("name", COMMAND, "alias1", "alias2", "...")
            .setUsage("[<var>] [value1 / value2] {<not needed but you can>}")
            .setDescription("Simple description")
            .setDetailedDescription("detailed description", "and more", "...");
    }

    private CommandTemplate() {
    }

    @Override
    public void execute(MinecraftInfo info, Arguments arguments) {

    }

    @Override
    public DefaultCompletion complete(MinecraftInfo info, Arguments arguments) {
        return new DefaultCompletion();
    }

}
