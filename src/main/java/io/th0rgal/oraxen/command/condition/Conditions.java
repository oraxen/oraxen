package io.th0rgal.oraxen.command.condition;

import org.bukkit.entity.Player;

import io.th0rgal.oraxen.command.permission.IPermission;
import io.th0rgal.oraxen.language.ITranslatable;
import io.th0rgal.oraxen.utils.general.Placeholder;

public class Conditions {

    public static MixedCommandCondition mixed(CommandCondition... conditions) {
        return new MixedCommandCondition(conditions);
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
