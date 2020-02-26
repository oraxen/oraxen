package io.th0rgal.oraxen.utils.message;

import org.bukkit.command.CommandSender;

public abstract class Message {

    private int delay = 0;

    public Message() {
    }

    public Message(int delay) {
        this.delay = delay;
    }

    public boolean delay() {
        if(delay == 0)
            return true;
        delay--;
        return false;
    }

    public boolean canSend(CommandSender sender) {
        return getAction().isAbleToReceive(sender);
    }

    public abstract String getContent();

    public abstract MessageAction getAction();

    public abstract void sendTo(CommandSender sender);

}
