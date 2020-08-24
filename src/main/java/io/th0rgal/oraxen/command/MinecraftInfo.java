package io.th0rgal.oraxen.command;

import org.bukkit.command.CommandSender;

import com.syntaxphoenix.syntaxapi.command.BaseInfo;
import com.syntaxphoenix.syntaxapi.command.CommandManager;

public class MinecraftInfo extends BaseInfo {

    private final CommandSender sender;
    private final CommandInfo info;

    public MinecraftInfo(CommandManager manager, String label, CommandInfo info, CommandSender sender) {
        super(manager, label);
        this.sender = sender;
        this.info = info;
    }

    public CommandInfo getInfo() {
        return info;
    }

    public CommandSender getSender() {
        return sender;
    }

}
