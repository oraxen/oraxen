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

    private String section;
    private static ResourcesManager resourcesManager = new ResourcesManager(OraxenPlugin.get());

    Pack(String section) {
        this.section = section;
    }

    public Object getValue() {
        return resourcesManager.getSettings().getConfigurationSection("Pack").get(section);
    }

    @Override
    public String toString() {
        return ChatColor.translateAlternateColorCodes('&', this.getValue().toString());
    }

}
