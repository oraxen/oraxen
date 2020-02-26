package io.th0rgal.oraxen.pack.receive;

import io.th0rgal.oraxen.settings.Pack;
import io.th0rgal.oraxen.utils.message.ComponentMessage;
import io.th0rgal.oraxen.utils.message.Message;
import io.th0rgal.oraxen.utils.message.MessageAction;
import io.th0rgal.oraxen.utils.message.MessageTimer;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.util.ArrayList;
import java.util.List;

public class PackReceiver implements Listener {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerUpdatesPackStatus(PlayerResourcePackStatusEvent event) {
        boolean message;
        int delay;
        List<BaseComponent[]> components;
        List<String> commands;
        MessageAction action;

        PlayerResourcePackStatusEvent.Status status = event.getStatus();
        if (status == PlayerResourcePackStatusEvent.Status.DECLINED){
            action = MessageAction.fromString((String) Pack.RECEIVE_DENIED_MESSAGE_ACTION.getValue());
            message = (boolean) Pack.RECEIVE_DENIED_SEND_MESSAGE.getValue();
            delay = (int) Pack.RECEIVE_DENIED_MESSAGE_DELAY.getValue();
            components = Pack.RECEIVE_DENIED_MESSAGE.toMiniMessageList();
            commands = (List<String>) Pack.RECEIVE_DENIED_COMMANDS.getValue();
        } else if(status == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD) {
            action = MessageAction.fromString((String) Pack.RECEIVE_FAILED_MESSAGE_ACTION.getValue());
            message = (boolean) Pack.RECEIVE_FAILED_SEND_MESSAGE.getValue();
            delay = (int) Pack.RECEIVE_FAILED_MESSAGE_DELAY.getValue();
            components = Pack.RECEIVE_FAILED_MESSAGE.toMiniMessageList();
            commands = (List<String>) Pack.RECEIVE_FAILED_COMMANDS.getValue();
        } else {
            action = MessageAction.fromString((String) Pack.RECEIVE_ALLOWED_MESSAGE_ACTION.getValue());
            message = (boolean) Pack.RECEIVE_ALLOWED_SEND_MESSAGE.getValue();
            delay = (int) Pack.RECEIVE_ALLOWED_MESSAGE_DELAY.getValue();
            components = Pack.RECEIVE_ALLOWED_MESSAGE.toMiniMessageList();
            commands = (List<String>) Pack.RECEIVE_ALLOWED_COMMANDS.getValue();
        }

        if (message && components.isEmpty()) {
            MessageTimer.getDefaultTimer().queue(event.getPlayer(), (List) ComponentMessage.convert(components, delay, action));
        }
        if (!commands.isEmpty())
            for(String command : commands)
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%p%", event.getPlayer().getName()));

    }

}
