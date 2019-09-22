package io.th0rgal.oraxen.settings;

import io.th0rgal.oraxen.utils.Logs;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;


public enum Message {

    NOT_A_PLAYER_ERROR(ChatColor.RED.toString() + "You must be a player to use this command!"),
    COMMAND_DOES_NOT_EXIST_ERROR(ChatColor.RED.toString() + "This command doesn't exist, check the doc!"),
    ZIP_BROWSE_ERROR(ChatColor.RED.toString() + "An error occured browsing the zip"),
    MECHANIC_DOESNT_EXIST(ChatColor.RED.toString() + "The mechanic {mechanic} doesn't exist"),
    WRONG_TYPE(ChatColor.RED.toString() + "You are using an invalid type");

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

    public void log() {
        Logs.log(toString());
    }

    public void logWarning() {
        Logs.logWarning(toString());
    }

    public void logError() {
        Logs.logError(toString());
    }

}
