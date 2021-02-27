package org.playuniverse.snowypine.module;

import org.playuniverse.snowypine.command.Command;
import org.playuniverse.snowypine.command.CommandInfo;

public abstract class ModuleCommand extends Command {

	protected abstract CommandInfo buildInfo(Command command);

}
