package io.th0rgal.oraxen.utils.message;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public enum MessageAction {

    KICK(true),
    CHAT(false),
    TITLE(true),
    ACTION_BAR(ChatMessageType.ACTION_BAR),
    BOSS_BAR(true);

    private final boolean onlyPlayer;
    private ChatMessageType type;

    private MessageAction(boolean onlyPlayer) {
        this.onlyPlayer = onlyPlayer;
    }

    private MessageAction(ChatMessageType type) {
        this.onlyPlayer = true;
        this.type = type;
    }

    public boolean isAbleToReceive(CommandSender sender) {
        if(onlyPlayer && !(sender instanceof Player))
            return true;
        else
            return !onlyPlayer;
    }

    public boolean hasType() {
        return type != null;
    }

    public ChatMessageType getType() {
        return type;
    }

    public static MessageAction fromString(String name) {
        for(MessageAction action : values()) {
            if (action.name().equalsIgnoreCase(name))
                return action;
        }
        return CHAT;
    }
}
