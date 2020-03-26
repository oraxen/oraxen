package io.th0rgal.oraxen.utils.message;

import net.md_5.bungee.api.ChatMessageType;

public enum MessageAction {

    KICK(true),
    CHAT(false),
    TITLE(true),
    ACTION_BAR(ChatMessageType.ACTION_BAR),
    BOSS_BAR(true);

    private final boolean onlyPlayer;
    private ChatMessageType type;

    MessageAction(boolean onlyPlayer) {
        this.onlyPlayer = onlyPlayer;
    }

    MessageAction(ChatMessageType type) {
        this.onlyPlayer = true;
        this.type = type;
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
