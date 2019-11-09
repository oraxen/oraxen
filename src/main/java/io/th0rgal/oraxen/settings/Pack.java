package io.th0rgal.oraxen.settings;

import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.ChatColor;

public enum Pack implements ConfigEnum {
    GENERATE("generate"),
    UPLOAD("upload"),
    SEND("send"),
    COMPRESSION("compression"),
    COMMENT("comment");

    private final String section;
    private static final ResourcesManager RESOURCES_MANAGER = new ResourcesManager(OraxenPlugin.get());

    Pack(String section) {
        this.section = section;
    }

    public Object getValue() {
        return RESOURCES_MANAGER.getSettings().getConfigurationSection("Pack").get(section);
    }

    @Override
    public String toString() {
        return ChatColor.translateAlternateColorCodes('&', this.getValue().toString());
    }

}
