package org.playuniverse.snowypine.command;

import org.bukkit.command.CommandSender;

import com.syntaxphoenix.syntaxapi.command.BaseInfo;
import com.syntaxphoenix.syntaxapi.command.CommandManager;

public class MinecraftInfo extends BaseInfo {

	private final CommandProvider provider;
	private final CommandSender sender;
	private final CommandInfo info;

	public MinecraftInfo(CommandProvider provider, CommandManager manager, String label, CommandInfo info, CommandSender sender) {
		super(manager, label);
		this.provider = provider;
		this.sender = sender;
		this.info = info;
	}

	public CommandProvider getProvider() {
		return provider;
	}

	public CommandInfo getInfo() {
		return info;
	}

	public CommandSender getSender() {
		return sender;
	}

}
