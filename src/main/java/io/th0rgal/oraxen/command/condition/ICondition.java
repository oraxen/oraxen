package io.th0rgal.oraxen.command.condition;

import java.util.function.Predicate;

import org.bukkit.command.CommandSender;

@FunctionalInterface
public interface ICondition extends Predicate<CommandSender> {

    @Override
    default boolean test(CommandSender sender) {
        return isTrue(sender);
    }

    public default boolean isFalse(CommandSender sender) {
        return !isTrue(sender);
    }

    public boolean isTrue(CommandSender sender);

}
