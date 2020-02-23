package io.th0rgal.oraxen.utils.message;

import io.th0rgal.oraxen.OraxenPlugin;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.boss.BarColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ComponentMessage extends Message {

    private final BaseComponent[] message;
    private MessageAction action;

    public ComponentMessage(BaseComponent... message) {
        this.message = message;
    }

    public ComponentMessage(int delay, BaseComponent... message) {
        super(delay);
        this.message = message;
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
        if(action == MessageAction.CHAT)
            sender.spigot().sendMessage(message);
        else if(!canSend(sender))
            return;
        Player player = (Player) sender;
        if(action.hasType()) {
            player.spigot().sendMessage(action.getType(), message);
            return;
        }
        if(action == MessageAction.KICK)
            player.kickPlayer(getContent());
        if(action == MessageAction.TITLE)
            player.sendTitle(stringifyComponents(message), "", 20, 100, 20);
        else {
            BossbarMessager messager = MessageTimer.getDefaultTimer().getBossbarManager().getFreeMessager();
            messager.apply(player);
            messager.setProgress(1);
            messager.setColor(BarColor.RED);
            messager.setTitle(stringifyComponents(message));
            messager.setVisible(true);
        }
    }

    public static String stringifyComponents(BaseComponent[] message) {
        StringBuilder builder = new StringBuilder();
        for(BaseComponent component : message)
            builder.append(component.toLegacyText());
        return builder.toString();
    }

    public static List<ComponentMessage> convert(List<BaseComponent[]> componentList) {
        ArrayList<ComponentMessage> messages = new ArrayList<>(componentList.size());
        if(componentList.isEmpty())
            return messages;
        for(BaseComponent[] components : componentList)
            messages.add(new ComponentMessage(components));
        return messages;
    }

    public static List<ComponentMessage> convert(List<BaseComponent[]> componentList, int delay) {
        ArrayList<ComponentMessage> messages = new ArrayList<>(componentList.size());
        if(componentList.isEmpty())
            return messages;
        for(BaseComponent[] components : componentList)
            messages.add(new ComponentMessage(delay, components));
        return messages;
    }

}
