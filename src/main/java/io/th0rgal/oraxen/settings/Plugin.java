package io.th0rgal.oraxen.settings;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.Utils;
import org.bukkit.ChatColor;

import java.util.List;

public enum Plugin implements ConfigEnum {

    REPAIR_COMMAND_ORAXEN_DURABILITY("Plugin.commands.repair.oraxen_durability_only"),

    CONFIGS_VERSION("configs_version"),
    HEX_SUPPORTED("ConfigsTools.hexColorCodes.enabled_support"),
    HEX_PREFIX("ConfigsTools.hexColorCodes.prefix"),
    HEX_SUFFIX("ConfigsTools.hexColorCodes.suffix"),
    UPDATE_CONFIGS("ConfigsTools.enable_configs_updater"),
    AUTOMATICALLY_SET_MODEL_ID("ConfigsTools.automatically_set_model_id"),
    ERROR_ITEM("ConfigsTools.error_item"),

    RESET_RECIPES("Misc.reset_recipes"),
    ARMOR_EQUIP_EVENT_BYPASS("Misc.armor_equip_event_bypass"),
    SHIELD_DISPLAY("Misc.shield_display"),
    BOW_DISPLAY("Misc.bow_display");

    private final Object value;

    Plugin(String section) {
        this.value = new ResourcesManager(OraxenPlugin.get()).getSettings().get(section);
    }

    public Object getValue() {
        return this.value;
    }

    @SuppressWarnings("unchecked")
    public List<String> getAsStringList() {
        return (List<String>) value;
    }

    @Override
    public String toString() {
        return Utils.handleColors(this.value.toString());
    }

}