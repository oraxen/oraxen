package io.th0rgal.oraxen.command.condition;

import java.util.Arrays;

import org.bukkit.command.CommandSender;

import com.mojang.brigadier.context.CommandContext;

public class MixedCondition implements ICondition {

    private final ICondition[] conditions;

    public MixedCondition(ICondition... conditions) {
        this.conditions = conditions;
    }

    @Override
    public boolean isTrue(CommandSender sender, CommandContext<CommandSender> context) {
        return Arrays.stream(conditions).allMatch(condition -> condition.isTrue(sender, context));
    }

}
