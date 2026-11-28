package io.th0rgal.oraxen.pack.receive;

import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.commands.CommandsParser;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Locale;

public class PackAction {

    public enum MessageType {
        KICK, CHAT, ACTION_BAR, TITLE
    }

    private final int delay;

    private String soundType;
    private float soundVolume;
    private float soundPitch;

    private MessageType messageType;
    private Component messageContent;

    private final CommandsParser commandsParser;

    public PackAction(ConfigurationSection configurationSection, TagResolver tagResolver) {

        delay = configurationSection.getInt("delay", 0);

        if (configurationSection.isConfigurationSection("sound")) {
            ConfigurationSection soundSection = configurationSection.getConfigurationSection("sound");
            assert soundSection != null;
            soundType = !soundSection.getBoolean("enabled", true) ? null : soundSection.getString("type");
            soundVolume = (float) soundSection.getDouble("volume");
            soundPitch = (float) soundSection.getDouble("pitch");
        }

        if (configurationSection.isConfigurationSection("message")) {
            ConfigurationSection messageSection = configurationSection.getConfigurationSection("message");
            assert messageSection != null;
            if (messageSection.getBoolean("enabled", false)) {
                messageType = parseMessageType(messageSection);
                messageContent = AdventureUtils.MINI_MESSAGE.deserialize(messageSection.getString("content", ""), tagResolver);
            }
        }

        commandsParser = new CommandsParser(configurationSection.getConfigurationSection("commands"), tagResolver);
    }

    private static MessageType parseMessageType(ConfigurationSection messageSection) {
        String type = messageSection.getString("type");
        if (type != null) {
            try {
                return MessageType.valueOf(type.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }
        Logs.logWarning("Invalid message type '" + type + "' at " + messageSection.getCurrentPath() + ".type. Falling back to CHAT.");
        return MessageType.CHAT;
    }

    public int getDelay() {
        return delay;
    }

    public boolean hasSound() {
        return soundType != null;
    }

    public void playSound(Player player, Location location) {
        AdventureUtils.playSound(player, location, soundType, Sound.Source.MASTER, soundVolume, soundPitch);
    }

    public boolean hasMessage() {
        return messageContent != null;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public Component getMessageContent() {
        return messageContent;
    }

    public CommandsParser getCommandsParser() {
        return commandsParser;
    }
}
