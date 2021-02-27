package org.playuniverse.snowypine.command;

import com.syntaxphoenix.syntaxapi.command.Arguments;
import com.syntaxphoenix.syntaxapi.command.BaseCommand;
import com.syntaxphoenix.syntaxapi.command.BaseInfo;
import com.syntaxphoenix.syntaxapi.command.DefaultCompletion;

public abstract class Command extends BaseCommand {

	@Override
	public final void execute(BaseInfo info, Arguments arguments) {
		if (info instanceof MinecraftInfo) {
			execute((MinecraftInfo) info, arguments);
		}
	}

	public final void run(MinecraftInfo info, Arguments arguments) {
		CommandInfo command = info.getInfo();
		if (command.getPermission().required(info.getSender())) {
			return;
		}
		execute(info, arguments);
	}

	public abstract void execute(MinecraftInfo info, Arguments arguments);

	public abstract DefaultCompletion complete(MinecraftInfo info, Arguments arguments);

}
