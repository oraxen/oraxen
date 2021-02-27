package org.playuniverse.snowypine.command.condition;

import org.bukkit.command.CommandSender;

public class MixedCommandOrCondition extends MixedOrCondition<CommandSender> {

	public MixedCommandOrCondition(CommandCondition... conditions) {
		super(conditions);
	}

}
