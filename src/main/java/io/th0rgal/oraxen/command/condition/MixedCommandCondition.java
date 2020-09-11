package io.th0rgal.oraxen.command.condition;

import org.bukkit.command.CommandSender;

public class MixedCommandCondition extends MixedCondition<CommandSender> {

    public MixedCommandCondition(CommandCondition... conditions) {
        super(conditions);
    }
    
}
