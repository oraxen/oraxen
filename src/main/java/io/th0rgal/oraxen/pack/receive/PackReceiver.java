package io.th0rgal.oraxen.pack.receive;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.settings.Pack;
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

import java.util.List;

public class PackReceiver implements Listener {

    @SuppressWarnings("rawtypes")
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerUpdatesPackStatus(PlayerResourcePackStatusEvent event) {
        boolean message;
        int delay;
        List<BaseComponent[]> components;
        CommandsParser commands;
        MessageAction action;

        PlayerResourcePackStatusEvent.Status status = event.getStatus();

        switch (status) {

            case ACCEPTED:
                action = MessageAction.fromString((String) Pack.RECEIVE_ALLOWED_MESSAGE_ACTION.getValue());
                message = (boolean) Pack.RECEIVE_ALLOWED_SEND_MESSAGE.getValue();
                delay = (int) Pack.RECEIVE_ALLOWED_MESSAGE_DELAY.getValue();
                components = Pack.RECEIVE_ALLOWED_MESSAGE.toMiniMessageList();
                commands = new CommandsParser((ConfigurationSection) Pack.RECEIVE_ALLOWED_COMMANDS.getValue());
                break;

            case DECLINED:
                action = MessageAction.fromString((String) Pack.RECEIVE_DENIED_MESSAGE_ACTION.getValue());
                message = (boolean) Pack.RECEIVE_DENIED_SEND_MESSAGE.getValue();
                delay = (int) Pack.RECEIVE_DENIED_MESSAGE_DELAY.getValue();
                components = Pack.RECEIVE_DENIED_MESSAGE.toMiniMessageList();
                commands = new CommandsParser((ConfigurationSection) Pack.RECEIVE_ALLOWED_COMMANDS.getValue());
                break;

            case FAILED_DOWNLOAD:
                action = MessageAction.fromString((String) Pack.RECEIVE_FAILED_MESSAGE_ACTION.getValue());
                message = (boolean) Pack.RECEIVE_FAILED_SEND_MESSAGE.getValue();
                delay = (int) Pack.RECEIVE_FAILED_MESSAGE_DELAY.getValue();
                components = Pack.RECEIVE_FAILED_MESSAGE.toMiniMessageList();
                commands = new CommandsParser((ConfigurationSection) Pack.RECEIVE_ALLOWED_COMMANDS.getValue());
                break;

            case SUCCESSFULLY_LOADED:
                action = MessageAction.fromString((String) Pack.RECEIVE_LOADED_MESSAGE_ACTION.getValue());
                message = (boolean) Pack.RECEIVE_LOADED_SEND_MESSAGE.getValue();
                delay = (int) Pack.RECEIVE_LOADED_MESSAGE_DELAY.getValue();
                components = Pack.RECEIVE_LOADED_MESSAGE.toMiniMessageList();
                commands = new CommandsParser((ConfigurationSection) Pack.RECEIVE_ALLOWED_COMMANDS.getValue());
                break;

            default:
                throw new IllegalStateException("Unexpected value: " + status);
        }

        if (message && !components.isEmpty())
            sendMessageLoop(event.getPlayer(), ComponentMessage.convert(components, delay, action));

        commands.perform(event.getPlayer());
    }

    private void sendMessageLoop(CommandSender receiver, List<ComponentMessage> messages) {
        ComponentMessage nextMessage = messages.remove(0);
        Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () -> {
            nextMessage.sendTo(receiver);
            if (!messages.isEmpty())
                sendMessageLoop(receiver, messages);
        }, nextMessage.getDelay() * 20);
    }

}