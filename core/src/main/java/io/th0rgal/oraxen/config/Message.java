package io.th0rgal.oraxen.config;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.AdventureUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

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
    REMOVING_CONFIG("general.removing_config"),
    CONFIGS_VALIDATION_FAILED("general.configs_validation_failed"),
    REPAIRED_ITEMS("general.repaired_items"),
    CANNOT_BE_REPAIRED("general.cannot_be_repaired"),
    CANNOT_BE_REPAIRED_INVALID("general.cannot_be_repaired_invalid"),
    UPDATED_ITEMS("general.updated_items"),
    ZIP_BROWSE_ERROR("general.zip_browse_error"),
    BAD_RECIPE("general.bad_recipe"),
    ITEM_NOT_FOUND("general.item_not_found"),
    PLUGIN_HOOKS("general.plugin_hooks"),
    PLUGIN_UNHOOKS("general.plugin_unhooks"),
    NOT_ENOUGH_EXP("general.not_enough_exp"),
    NOT_ENOUGH_SPACE("general.not_enough_space"),
    EXIT_MENU("general.exit_menu"),
    NO_EMOJIS("general.no_emojis"),

    // logs
    PLUGIN_LOADED("logs.loaded"),
    PLUGIN_UNLOADED("logs.unloaded"),
    NO_ARMOR_ITEM("logs.no_armor_item"),
    DUPLICATE_ARMOR_COLOR("logs.duplicate_armor_color"),

    // command
    COMMAND_HELP("command.help"),
    COMMAND_JOIN_MESSAGE("command.join"),
    DEBUG_TOGGLE("command.debug.toggle"),
    VERSION("command.version"),

    RECIPE_NO_BUILDER("command.recipe.no_builder"),
    RECIPE_NO_FURNACE("command.recipe.no_furnace"),
    RECIPE_NO_NAME("command.recipe.no_name"),
    RECIPE_NO_RECIPE("command.recipe.no_recipes"),
    RECIPE_NO_ITEM("command.recipe.no_item"),
    RECIPE_SAVE("command.recipe.save"),

    GIVE_PLAYER("command.give.player"),
    GIVE_PLAYERS("command.give.players"),

    DYE_SUCCESS("command.dye.success"),
    DYE_WRONG_COLOR("command.dye.wrong_color"),
    DYE_FAILED("command.dye.failed"),

    HUD_NO_HUD("command.hud.no_hud"),
    HUD_TOGGLE_ON("command.hud.toggle_on"),
    HUD_TOGGLE_OFF("command.hud.toggle_off"),

    GESTURE_CONSOLE("command.gesture.console"),
    GESTURE_NO_GESTURE("command.gesture.no_gesture"),
    GESTURE_OTHERS_DENIED("command.gesture.others_denied"),

    // mechanics
    MECHANICS_NOT_ENOUGH_EXP("mechanics.not_enough_exp"),
    MECHANICS_BACKPACK_STACKED("mechanics.backpack_stacked"),
    MECHANICS_JUKEBOX_NOW_PLAYING("mechanics.jukebox_now_playing");

    private final String path;

    Message(final String path) {
        this.path = path;
    }


    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return OraxenPlugin.get().getConfigsManager().getLanguage().getString(path);
    }

    public void send(final CommandSender sender, final TagResolver... placeholders) {
        if (sender == null) return;
        String lang = OraxenPlugin.get().getConfigsManager().getLanguage().getString(path);
        if (lang == null || lang.isEmpty()) return;
        OraxenPlugin.get().getAudience().sender(sender).sendMessage(
                AdventureUtils.MINI_MESSAGE.deserialize(lang, TagResolver.resolver(placeholders))
        );
    }

    @NotNull
    public final Component toComponent() {
        return AdventureUtils.MINI_MESSAGE.deserialize(toString());
    }

    @NotNull
    public String toSerializedString() {
        return AdventureUtils.LEGACY_SERIALIZER.serialize(toComponent());
    }

    public void log(final TagResolver... placeholders) {
        send(Bukkit.getConsoleSender(), placeholders);
    }

}
