package org.playuniverse.snowypine.command.condition;

import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.playuniverse.snowypine.command.permission.IPermission;
import org.playuniverse.snowypine.language.ITranslatable;
import org.playuniverse.snowypine.utils.general.Placeholder;

public class Conditions {

	public static MixedCommandAndCondition and(CommandCondition... conditions) {
		return new MixedCommandAndCondition(conditions);
	}

	public static MixedCommandOrCondition or(CommandCondition... conditions) {
		return new MixedCommandOrCondition(conditions);
	}

	public static CommandCondition hasPerm(IPermission permission) {
		return (sender) -> permission.has(sender);
	}

	public static CommandCondition reqPerm(IPermission permission) {
		return (sender) -> permission.required(sender);
	}

	public static CommandCondition reqPerm(IPermission permission, Placeholder... placeholders) {
		return (sender) -> permission.required(sender, placeholders);
	}

	public static CommandCondition console() {
		return (sender) -> sender instanceof ConsoleCommandSender;
	}

	public static CommandCondition player() {
		return (sender) -> sender instanceof Player;
	}

	public static CommandCondition player(ITranslatable translatable, Placeholder... placeholders) {
		return (sender) -> {
			if (!(sender instanceof Player)) {
				translatable.send(sender, placeholders);
				return false;
			}
			return true;
		};
	}

}
