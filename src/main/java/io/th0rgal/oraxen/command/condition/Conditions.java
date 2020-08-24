package io.th0rgal.oraxen.command.condition;

import org.bukkit.entity.Player;

import io.th0rgal.oraxen.command.permission.IPermission;
import io.th0rgal.oraxen.language.ITranslatable;
import io.th0rgal.oraxen.utils.general.Placeholder;

public class Conditions {

    public static MixedCondition mixed(ICondition... conditions) {
        return new MixedCondition(conditions);
    }

    public static ICondition hasPerm(IPermission permission) {
        return (sender) -> permission.has(sender);
    }

    public static ICondition reqPerm(IPermission permission) {
        return (sender) -> permission.required(sender);
    }

    public static ICondition reqPerm(IPermission permission, Placeholder... placeholders) {
        return (sender) -> permission.required(sender, placeholders);
    }

    public static ICondition player() {
        return (sender) -> sender instanceof Player;
    }

    public static ICondition player(ITranslatable translatable, Placeholder... placeholders) {
        return (sender) -> {
            if (!(sender instanceof Player)) {
                translatable.send(sender, placeholders);
                return false;
            }
            return true;
        };
    }

}
