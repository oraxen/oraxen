package io.th0rgal.oraxen.utils.message;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ComponentMessage extends Message {

    private final BaseComponent[] message;
    private MessageAction action;

    public ComponentMessage(BaseComponent... message) {
        this(0, message);
    }

    public ComponentMessage(int delay, BaseComponent... message) {
        this(MessageAction.CHAT, delay, message);
    }

    public ComponentMessage(MessageAction action, int delay, BaseComponent... message) {
        super(delay);
        this.message = message;
        this.action = action;
    }

    public BaseComponent[] getMessage() {
        return message;
    }

    @Override
    public String getContent() {
        return stringifyComponents(message);
    }

    @Override
    public MessageAction getAction() {
        return action;
    }

    @Override
    public void sendTo(CommandSender sender) {
        Player player = (Player) sender;
        if (action == MessageAction.CHAT)
            sender.spigot().sendMessage(message);
        if (action.hasType()) {
            player.spigot().sendMessage(action.getType(), message);
            return;
        }
        if (action == MessageAction.KICK)
            player.kickPlayer(getContent());
        if (action == MessageAction.TITLE)
            player.sendTitle(stringifyComponents(message), "", 20, 100, 20);
    }

    public static String stringifyComponents(BaseComponent[] message) {
        StringBuilder builder = new StringBuilder();
        for (BaseComponent component : message)
            builder.append(component.toLegacyText());
        return builder.toString();
    }

    public static List<ComponentMessage> convert(List<BaseComponent[]> componentList) {
        ArrayList<ComponentMessage> messages = new ArrayList<>(componentList.size());
        if (componentList.isEmpty())
            return messages;
        for (BaseComponent[] components : componentList)
            messages.add(new ComponentMessage(components));
        return messages;
    }

    public static List<ComponentMessage> convert(List<BaseComponent[]> componentList, int delay) {
        ArrayList<ComponentMessage> messages = new ArrayList<>(componentList.size());
        if (componentList.isEmpty())
            return messages;
        for (BaseComponent[] components : componentList)
            messages.add(new ComponentMessage(delay, components));
        return messages;
    }

    public static List<ComponentMessage> convert(List<BaseComponent[]> componentList, int delay, MessageAction action) {
        ArrayList<ComponentMessage> messages = new ArrayList<>(componentList.size());
        if (componentList.isEmpty())
            return messages;
        for (BaseComponent[] components : componentList)
            messages.add(new ComponentMessage(action, delay, components));
        return messages;
    }

}