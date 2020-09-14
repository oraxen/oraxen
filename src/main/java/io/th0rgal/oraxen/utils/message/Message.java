package io.th0rgal.oraxen.utils.message;

import org.bukkit.command.CommandSender;

public abstract class Message {

    private long delay = 0;

    public Message() {
    }

    public Message(int delay) {
        this.delay = delay;
    }

    public long getDelay() {
        return delay;
    }

    public abstract String getContent();

    public abstract MessageAction getAction();

    public abstract void sendTo(CommandSender sender);

}
