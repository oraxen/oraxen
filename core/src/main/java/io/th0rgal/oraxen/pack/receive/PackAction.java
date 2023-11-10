package io.th0rgal.oraxen.pack.receive;

import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.commands.CommandsParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class PackAction {

    private final int delay;

    private String soundType;
    private float soundVolume;
    private float soundPitch;

    private String messageType;
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
            messageType = messageSection.getString("type");
            if (messageSection.getBoolean("enabled", true)) {
                messageContent = AdventureUtils.MINI_MESSAGE.deserialize(messageSection.getString("content", ""), tagResolver);
            }
        }

        commandsParser = new CommandsParser(configurationSection.getConfigurationSection("commands"), tagResolver);
    }

    public int getDelay() {
        return delay;
    }

    public boolean hasSound() {
        return soundType != null;
    }

    public void playSound(Player player, Location location) {
        player.playSound(location, soundType, soundVolume, soundPitch);
    }

    public boolean hasMessage() {
        return messageContent != null;
    }

    public String getMessageType() {
        return messageType;
    }

    public Component getMessageContent() {
        return messageContent;
    }

    public CommandsParser getCommandsParser() {
        return commandsParser;
    }
}
