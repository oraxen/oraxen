package org.playuniverse.snowypine.command.commands;

import org.playuniverse.snowypine.command.Command;
import org.playuniverse.snowypine.command.CommandInfo;
import org.playuniverse.snowypine.command.MinecraftInfo;

import com.syntaxphoenix.syntaxapi.command.Arguments;
import com.syntaxphoenix.syntaxapi.command.DefaultCompletion;

public final class CommandTemplate extends Command {

    public static final Command COMMAND = new CommandTemplate();

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
