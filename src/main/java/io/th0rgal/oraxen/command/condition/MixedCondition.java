package io.th0rgal.oraxen.command.condition;

import java.util.Arrays;

import org.bukkit.command.CommandSender;

public class MixedCondition implements ICondition {

    private final ICondition[] conditions;

    public MixedCondition(ICondition... conditions) {
        this.conditions = conditions;
    }

    @Override
    public boolean isTrue(CommandSender sender) {
        return Arrays.stream(conditions).allMatch(condition -> condition.isTrue(sender));
    }

}
