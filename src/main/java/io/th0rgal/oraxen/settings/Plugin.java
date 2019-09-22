package io.th0rgal.oraxen.settings;

import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.ChatColor;

public enum Plugin implements ConfigEnum {

    NAME("Plugin.name"),
    PREFIX("Plugin.prefix");

    private Object value;

    Plugin(String section) {
        this.value = new ResourcesManager(OraxenPlugin.get()).getSettings().get(section);
    }

    public Object getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return ChatColor.translateAlternateColorCodes('&', this.value.toString());
    }

}