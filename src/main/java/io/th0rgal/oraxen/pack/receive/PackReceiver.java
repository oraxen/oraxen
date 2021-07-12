package io.th0rgal.oraxen.pack.receive;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.commands.CommandsParser;
import io.th0rgal.oraxen.utils.message.ComponentMessage;
import io.th0rgal.oraxen.utils.message.MessageAction;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.util.ArrayList;
import java.util.List;

public class PackReceiver implements Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerUpdatesPackStatus(PlayerResourcePackStatusEvent event) {
        boolean message;
        int delay;
        int period;
        List<BaseComponent[]> components;
        CommandsParser commands;
        MessageAction action;

        PlayerResourcePackStatusEvent.Status status = event.getStatus();

        switch (status) {

            case ACCEPTED:
                action = MessageAction.fromString(Settings.RECEIVE_ALLOWED_MESSAGE_ACTION.toString());
                message = Settings.RECEIVE_ALLOWED_SEND_MESSAGE.toBool();
                delay = (int) Settings.RECEIVE_ALLOWED_MESSAGE_DELAY.getValue();
                period = (int) Settings.RECEIVE_ALLOWED_MESSAGE_PERIOD.getValue();
                components = new ArrayList<>();
                commands = new CommandsParser((ConfigurationSection) Settings.RECEIVE_ALLOWED_COMMANDS.getValue());
                break;

            case DECLINED:
                action = MessageAction.fromString(Settings.RECEIVE_DENIED_MESSAGE_ACTION.toString());
                message = Settings.RECEIVE_DENIED_SEND_MESSAGE.toBool();
                delay = (int) Settings.RECEIVE_DENIED_MESSAGE_DELAY.getValue();
                period = (int) Settings.RECEIVE_DENIED_MESSAGE_PERIOD.getValue();
                components = new ArrayList<>();
                commands = new CommandsParser((ConfigurationSection) Settings.RECEIVE_ALLOWED_COMMANDS.getValue());
                break;

            case FAILED_DOWNLOAD:
                action = MessageAction.fromString(Settings.RECEIVE_FAILED_MESSAGE_ACTION.toString());
                message = Settings.RECEIVE_FAILED_SEND_MESSAGE.toBool();
                delay = (int) Settings.RECEIVE_FAILED_MESSAGE_DELAY.getValue();
                period = (int) Settings.RECEIVE_FAILED_MESSAGE_PERIOD.getValue();
                components = new ArrayList<>();
                commands = new CommandsParser((ConfigurationSection) Settings.RECEIVE_ALLOWED_COMMANDS.getValue());
                break;

            case SUCCESSFULLY_LOADED:
                action = MessageAction.fromString(Settings.RECEIVE_LOADED_MESSAGE_ACTION.toString());
                message = Settings.RECEIVE_LOADED_SEND_MESSAGE.toBool();
                delay = (int) Settings.RECEIVE_LOADED_MESSAGE_DELAY.getValue();
                period = (int) Settings.RECEIVE_LOADED_MESSAGE_PERIOD.getValue();
                components = new ArrayList<>();
                commands = new CommandsParser((ConfigurationSection) Settings.RECEIVE_ALLOWED_COMMANDS.getValue());
                break;

            default:
                throw new IllegalStateException("Unexpected value: " + status);
        }

        if (message && !components.isEmpty())
            Bukkit
                    .getScheduler()
                    .runTaskLater(OraxenPlugin.get(),
                            () -> sendMessageLoop(event.getPlayer(), ComponentMessage.convert(components, period, action)),
                            delay);

        commands.perform(event.getPlayer());
    }

    private void sendMessageLoop(CommandSender receiver, List<ComponentMessage> messages) {
        ComponentMessage nextMessage = messages.remove(0);
        nextMessage.sendTo(receiver);
        if (!messages.isEmpty())
            Bukkit
                    .getScheduler()
                    .runTaskLater(OraxenPlugin.get(), () -> sendMessageLoop(receiver, messages),
                            nextMessage.getDelay() * 20L);
    }

}