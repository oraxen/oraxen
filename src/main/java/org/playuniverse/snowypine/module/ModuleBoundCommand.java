package org.playuniverse.snowypine.module;

import org.pf4j.PluginWrapper;
import org.playuniverse.snowypine.command.Command;
import org.playuniverse.snowypine.command.MinecraftInfo;

import com.syntaxphoenix.syntaxapi.command.Arguments;
import com.syntaxphoenix.syntaxapi.command.DefaultCompletion;

public class ModuleBoundCommand extends Command {

	private final PluginWrapper wrapper;
	private final Command command;

	public ModuleBoundCommand(PluginWrapper wrapper, Command command) {
		this.wrapper = wrapper;
		this.command = command;
	}

	public PluginWrapper getWrapper() {
		return wrapper;
	}

	public Command getCommand() {
		return command;
	}

	/*
	 * BaseCommand Implementation
	 */

	@Override
	public void execute(MinecraftInfo info, Arguments arguments) {
		command.execute(info, arguments);
	}

	@Override
	public DefaultCompletion complete(MinecraftInfo info, Arguments arguments) {
		return command.complete(info, arguments);
	}

}
