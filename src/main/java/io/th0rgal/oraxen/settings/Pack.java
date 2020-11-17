package io.th0rgal.oraxen.settings;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.minimessage.MiniMessageParser;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public enum Pack implements ConfigEnum {

    GENERATE("generation.generate"),
    COMPRESSION("generation.compression"),
    COMMENT("generation.comment"),

    UPLOAD_TYPE("upload.type"),
    UPLOAD("upload.enabled"),
    UPLOAD_OPTIONS("upload.options"),

    POLYMATH_SERVER("upload.polymath.server"),

    SEND_PACK("dispatch.send_pack"),
    SEND_JOIN_MESSAGE("dispatch.join_message.enabled"),
    JOIN_MESSAGE_DELAY("dispatch.join_message.delay"),
    JOIN_MESSAGE_CONTENT("dispatch.join_message.content"),

    RECEIVE_ENABLED("receive.enabled"),

    RECEIVE_ALLOWED_SEND_MESSAGE("receive.accepted.actions.message.enabled"),
    RECEIVE_ALLOWED_MESSAGE_PERIOD("receive.accepted.actions.message.period"),
    RECEIVE_ALLOWED_MESSAGE_DELAY("receive.accepted.actions.message.delay"),
    RECEIVE_ALLOWED_MESSAGE_ACTION("receive.accepted.actions.message.type"),
    RECEIVE_ALLOWED_MESSAGE("receive.accepted.actions.message.messages"),
    RECEIVE_ALLOWED_COMMANDS("receive.accepted.actions.commands"),

    RECEIVE_LOADED_SEND_MESSAGE("receive.loaded.actions.message.enabled"),
    RECEIVE_LOADED_MESSAGE_PERIOD("receive.loaded.actions.message.period"),
    RECEIVE_LOADED_MESSAGE_DELAY("receive.loaded.actions.message.delay"),
    RECEIVE_LOADED_MESSAGE_ACTION("receive.loaded.actions.message.type"),
    RECEIVE_LOADED_MESSAGE("receive.loaded.actions.message.messages"),
    RECEIVE_LOADED_COMMANDS("receive.loaded.actions.commands"),

    RECEIVE_FAILED_SEND_MESSAGE("receive.failed_download.actions.message.enabled"),
    RECEIVE_FAILED_MESSAGE_PERIOD("receive.failed_download.actions.message.period"),
    RECEIVE_FAILED_MESSAGE_DELAY("receive.failed_download.actions.message.delay"),
    RECEIVE_FAILED_MESSAGE_ACTION("receive.failed_download.actions.message.type"),
    RECEIVE_FAILED_MESSAGE("receive.failed_download.actions.message.messages"),
    RECEIVE_FAILED_COMMANDS("receive.failed_download.actions.commands"),

    RECEIVE_DENIED_SEND_MESSAGE("receive.denied.actions.message.enabled"),
    RECEIVE_DENIED_MESSAGE_PERIOD("receive.denied.actions.message.period"),
    RECEIVE_DENIED_MESSAGE_DELAY("receive.denied.actions.message.delay"),
    RECEIVE_DENIED_MESSAGE_ACTION("receive.denied.actions.message.type"),
    RECEIVE_DENIED_MESSAGE("receive.denied.actions.message.messages"),
    RECEIVE_DENIED_COMMANDS("receive.denied.actions.commands");

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
        return MiniMessageParser.parseFormat(Utils.handleColors(message), placeholders);
    }

    public List<BaseComponent[]> toMiniMessageList(String... placeholders) {
        ConfigurationSection config = RESOURCES_MANAGER.getSettings().getConfigurationSection("Pack");
        ArrayList<BaseComponent[]> messages = new ArrayList<>();
        if (config.isList(section))
            config
                .getStringList(section)
                .forEach(message -> messages
                    .add(MiniMessageParser
                        .parseFormat(Utils.handleColors(message), placeholders)));
        else
            messages
                .add(MiniMessageParser
                    .parseFormat(Utils.handleColors(config.getString(section)), placeholders));
        return messages;
    }

    @Override
    public String toString() {
        return Utils.handleColors(this.getValue().toString());
    }

}
