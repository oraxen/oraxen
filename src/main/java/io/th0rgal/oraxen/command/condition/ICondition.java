package io.th0rgal.oraxen.command.condition;

import org.bukkit.command.CommandSender;

import com.mojang.brigadier.context.CommandContext;

@FunctionalInterface
public interface ICondition {
    
    public boolean isTrue(CommandSender sender, CommandContext<CommandSender> context);
    
}
