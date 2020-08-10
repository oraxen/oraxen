package io.th0rgal.oraxen.command.commands;

import org.bukkit.command.CommandSender;

import com.oraxen.chimerate.commons.command.tree.nodes.Literal;
import com.oraxen.chimerate.commons.command.tree.nodes.Literal.Builder;

import io.th0rgal.oraxen.command.CommandInfo;

public class Recipes {

    public static CommandInfo build() {
        return new CommandInfo("name", info -> {
            Builder<CommandSender> builder = Literal.of(info.getName()).alias(info.getAliases());
            
            return builder;
        }, "alias1", "alias2", "...");
    }

}