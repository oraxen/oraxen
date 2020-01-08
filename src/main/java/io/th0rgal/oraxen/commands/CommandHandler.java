package io.th0rgal.oraxen.commands;

import io.th0rgal.oraxen.settings.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.HashMap;

public class CommandHandler implements CommandExecutor {

    private static final HashMap<String, CommandInterface> COMMANDS = new HashMap<>();

    public CommandHandler register(String name, CommandInterface cmd) {
        COMMANDS.put(name, cmd);
        return this;
    }

    public boolean exists(String name) {
        return COMMANDS.containsKey(name);
    }

    public CommandInterface getExecutor(String name) {
        return COMMANDS.get(name);
    }

    //All commands will have this in common.
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

        //If there aren't any arguments, what is the command name going to be?
        if (args.length == 0) {
            getExecutor("oraxen").onCommand(sender, cmd, commandLabel, args);
            return true;
        }

        //What if there are arguments in the command? Such as /example args
        // implicit if (args.length > 0)

        //If that argument exists in our registration in the onEnable();
        if (exists(args[0])) {

            //Get The executor with the name of args[0].
            getExecutor(args[0]).onCommand(sender, cmd, commandLabel, args);
            return true;
        } else {
            //We want to send a message to the sender if the command doesn't exist.
            Message.COMMAND_DOES_NOT_EXIST_ERROR.send(sender);
            return true;
        }

    }

}
