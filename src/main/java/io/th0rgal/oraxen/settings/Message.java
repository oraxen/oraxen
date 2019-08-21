package io.th0rgal.oraxen.settings;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;


public enum Message {

    NOT_A_PLAYER_ERROR(ChatColor.RED.toString() + "You must be a player to use this command!"),
    COMMAND_DOES_NOT_EXIST_ERROR(ChatColor.RED.toString() + "This command doesn't exist, check the doc!"),
    WRONG_TYPE(ChatColor.RED.toString() + "You are using an invalid type"),;

    private String message;

    Message(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return Plugin.PREFIX + message;
    }

    public void send(CommandSender sender) {
        sender.sendMessage(toString());
    }

}
