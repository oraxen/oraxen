package io.th0rgal.oraxen.settings;

import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.ChatColor;

public enum Pack implements ConfigEnum {

    URL("url"),
    SHA1("SHA1"),
    SEND("send"),
    GENERATE("generate"),
    COMPRESSION("compression"),
    COMMENT("comment");

    private Object value;

    Pack(String section) {
        this.value = new ResourcesManager(OraxenPlugin.get()).getSettings().getConfigurationSection("Pack").get(section);
    }

    public Object getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return ChatColor.translateAlternateColorCodes('&', this.value.toString());
    }

}