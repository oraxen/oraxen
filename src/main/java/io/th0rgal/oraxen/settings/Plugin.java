package io.th0rgal.oraxen.settings;

import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.ChatColor;

import java.util.List;

public enum Plugin implements ConfigEnum {

    NAME("Plugin.name"),
    PREFIX("Plugin.prefix"),
    ARMOR_EQUIP_EVENT_BYPASS("Misc.armor_equip_event_bypass");

    private final Object value;

    Plugin(String section) {
        this.value = new ResourcesManager(OraxenPlugin.get()).getSettings().get(section);
    }

    public Object getValue() {
        return this.value;
    }

    @SuppressWarnings("unchecked")
    public List<String> getAsStringList() {
        return (List<String>)value;
    }

    @Override
    public String toString() {
        return ChatColor.translateAlternateColorCodes('&', this.value.toString());
    }

}