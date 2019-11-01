package io.th0rgal.oraxen.settings;

import io.th0rgal.oraxen.utils.Logs;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;


public enum Message {

    CMD_HELP(ChatColor.GREEN, "Check the docs for command usage: https://docs.oraxen.com/usage/commands"),

    NOT_A_PLAYER_ERROR(ChatColor.RED, "You must be a player to use this command!"),
    COMMAND_DOES_NOT_EXIST_ERROR(ChatColor.RED, "This command doesn't exist, check the doc!"),
    ZIP_BROWSE_ERROR(ChatColor.RED, "An error occured browsing the zip"),
    DONT_HAVE_PERMISSION(ChatColor.RED, "You need the permission %s to do this"),
    MECHANIC_DOESNT_EXIST(ChatColor.RED, "The mechanic %s doesn't exist"),
    WRONG_TYPE(ChatColor.RED, "You are using an invalid type");

    private String message;

    Message(ChatColor color, String message) {
        this.message = color + message;
    }

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

    public void send(CommandSender sender, String... arguments) {
        sender.sendMessage(String.format(toString(), (Object[])arguments));
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
