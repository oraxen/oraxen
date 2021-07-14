package io.th0rgal.oraxen.pack.receive;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.commands.CommandsParser;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public class PackReceiver implements Listener {

    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.builder()
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerUpdatesPackStatus(PlayerResourcePackStatusEvent event) {
        boolean message;
        int delay;
        int period;
        Component component;
        CommandsParser commands;
        String action;

        PlayerResourcePackStatusEvent.Status status = event.getStatus();

        switch (status) {
            case ACCEPTED -> {
                action = Settings.RECEIVE_ALLOWED_MESSAGE_ACTION.toString();
                message = Settings.RECEIVE_ALLOWED_SEND_MESSAGE.toBool();
                delay = (int) Settings.RECEIVE_ALLOWED_MESSAGE_DELAY.getValue();
                component = getComponent(Settings.RECEIVE_ALLOWED_MESSAGE);
                commands = new CommandsParser((ConfigurationSection) Settings.RECEIVE_ALLOWED_COMMANDS.getValue());
            }
            case DECLINED -> {
                action = Settings.RECEIVE_DENIED_MESSAGE_ACTION.toString();
                message = Settings.RECEIVE_DENIED_SEND_MESSAGE.toBool();
                delay = (int) Settings.RECEIVE_DENIED_MESSAGE_DELAY.getValue();
                component = getComponent(Settings.RECEIVE_DENIED_MESSAGE);
                commands = new CommandsParser((ConfigurationSection) Settings.RECEIVE_ALLOWED_COMMANDS.getValue());
            }
            case FAILED_DOWNLOAD -> {
                action = Settings.RECEIVE_FAILED_MESSAGE_ACTION.toString();
                message = Settings.RECEIVE_FAILED_SEND_MESSAGE.toBool();
                delay = (int) Settings.RECEIVE_FAILED_MESSAGE_DELAY.getValue();
                component = getComponent(Settings.RECEIVE_FAILED_MESSAGE);
                commands = new CommandsParser((ConfigurationSection) Settings.RECEIVE_ALLOWED_COMMANDS.getValue());
            }
            case SUCCESSFULLY_LOADED -> {
                action = Settings.RECEIVE_LOADED_MESSAGE_ACTION.toString();
                message = Settings.RECEIVE_LOADED_SEND_MESSAGE.toBool();
                delay = (int) Settings.RECEIVE_LOADED_MESSAGE_DELAY.getValue();
                component = getComponent(Settings.RECEIVE_LOADED_MESSAGE);
                commands = new CommandsParser((ConfigurationSection) Settings.RECEIVE_ALLOWED_COMMANDS.getValue());
            }
            default -> throw new IllegalStateException("Unexpected value: " + status);
        }

        if (message)
            Bukkit
                    .getScheduler()
                    .runTaskLater(OraxenPlugin.get(),
                            () -> sendMessage(event.getPlayer(), action, component), delay * 20L);
        commands.perform(event.getPlayer());
    }

    private Component getComponent(Settings settings) {
        return MiniMessage.get().parse(settings.toString());
    }

    private void sendMessage(Player receiver, String action, Component message) {
        @NotNull Audience audience = OraxenPlugin.get().getAudience().sender(receiver);
        switch (action) {
            case "KICK" -> receiver.kickPlayer(serializer.serialize(message));
            case "CHAT" -> audience.sendMessage(message);
            case "ACTION_BAR" -> audience.sendActionBar(message);
            case "TITLE" -> audience.showTitle(Title.title(null, message,
                    Title.Times.of(Duration.ofMillis(250), Duration.ofMillis(3500), Duration.ofMillis(250))));
        }
    }

}