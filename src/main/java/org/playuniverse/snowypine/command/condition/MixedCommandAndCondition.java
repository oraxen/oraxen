package org.playuniverse.snowypine.command.condition;

import org.bukkit.command.CommandSender;

public class MixedCommandAndCondition extends MixedAndCondition<CommandSender> {

	public MixedCommandAndCondition(CommandCondition... conditions) {
		super(conditions);
	}

}
