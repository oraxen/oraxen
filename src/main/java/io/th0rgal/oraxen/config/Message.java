package io.th0rgal.oraxen.config;

import io.th0rgal.oraxen.OraxenPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public enum Message {

    // general
    PREFIX("general.prefix"),
    NO_PERMISSION("general.no_permission"),
    WORK_IN_PROGRESS("general.work_in_progress"),
    NOT_PLAYER("general.not_player"),
    COOLDOWN("general.cooldown"),
    RELOAD("general.reload"),
    PACK_UPLOADING("general.pack_uploading"),
    PACK_NOT_UPLOADED("general.pack_not_uploaded"),
    PACK_UPLOADED("general.pack_uploaded"),
    PACK_REGENERATED("general.pack_regenerated"),
    UPDATING_CONFIG("general.updating_config"),
    CONFIGS_VALIDATION_FAILED("general.configs_validation_failed"),
    REPAIRED_ITEMS("general.repaired_items"),
    CANNOT_BE_REPAIRED("general.cannot_be_repaired"),
    CANNOT_BE_REPAIRED_INVALID("general.cannot_be_repaired_invalid"),
    ZIP_BROWSE_ERROR("general.zip_browse_error"),
    DONT_HAVE_PERMISSION("general.dont_have_permission"),
    MECHANIC_DOESNT_EXIST("general.mechanic_doesnt_exist"),
    BAD_RECIPE("general.bad_recipe"),
    ITEM_NOT_FOUND("general.item_not_found"),
    PLUGIN_HOOKS("general.plugin_hooks"),
    PLUGIN_UNHOOKS("general.plugin_unhooks"),
    NOT_ENOUGH_EXP("general.not_enough_exp"),

    // logs
    PLUGIN_LOADED("logs.loaded"),
    PLUGIN_UNLOADED("logs.unloaded"),

    // command
    COMMAND_NOT_EXIST("command.not_exist"),
    COMMAND_HELP("command.help"),
    COMMAND_JOIN_MESSAGE("command.join"),

    RECIPE_NO_BUILDER("command.recipe.no_builder"),
    RECIPE_NO_FURNACE("command.recipe.no_furnace"),
    RECIPE_NO_NAME("command.recipe.no_name"),
    RECIPE_NO_RECIPE("command.recipe.no_recipes"),
    RECIPE_NO_ITEM("command.recipe.no_item"),
    RECIPE_SAVE("command.recipe.save"),

    GIVE_PLAYER("command.give.player"),

    // mechanics
    MECHANICS_NOT_ENOUGH_EXP("mechanics.not_enough_exp");

    private final String path;

    Message(String path) {
        this.path = path;
    }


    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return OraxenPlugin.get().getLanguage().getString(path);
    }

    public void send(CommandSender sender, String... placeholders) {
        OraxenPlugin.get().getAudience().sender(sender).sendMessage(
                MiniMessage.get().parse(OraxenPlugin.get().getLanguage().getString(path),
                        ArrayUtils.addAll(new String[]{"prefix", Message.PREFIX.toString()}, placeholders)));
    }

    public void log(String... placeholders) {
        send(Bukkit.getConsoleSender(), placeholders);
    }

}
