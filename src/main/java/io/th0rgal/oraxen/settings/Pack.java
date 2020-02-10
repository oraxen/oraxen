package io.th0rgal.oraxen.settings;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.logs.Logs;
import io.th0rgal.oraxen.utils.minimessage.MiniMessageParser;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

public enum Pack implements ConfigEnum {

    GENERATE("generation.generate"),
    COMPRESSION("generation.compression"),
    COMMENT("generation.comment"),

    UPLOAD_TYPE("upload.type"),
    UPLOAD("upload.enabled"),

    POLYMATH_SERVER("upload.polymath.server"),

    SEND_PACK("dispatch.send_pack"),
    SEND_WELCOME_MESSAGE("dispatch.send_welcome_message"),
    WELCOME_MESSAGE("dispatch.welcome_message");

    private final String section;
    private static final ResourcesManager RESOURCES_MANAGER = new ResourcesManager(OraxenPlugin.get());

    Pack(String section) {
        this.section = section;
    }

    public Object getValue() {
        return RESOURCES_MANAGER.getSettings().getConfigurationSection("Pack").get(section);
    }

    public BaseComponent[] toMiniMessage(String... placeholders) {
        ConfigurationSection config = RESOURCES_MANAGER.getSettings().getConfigurationSection("Pack");
        String message;
        if (config.isList(section))
            message = String.join("\n", config.getStringList(section));
        else
            message = config.getString(section);
        return MiniMessageParser.parseFormat(message, placeholders);
    }

    @Override
    public String toString() {
        return ChatColor.translateAlternateColorCodes('&', this.getValue().toString());
    }

}
