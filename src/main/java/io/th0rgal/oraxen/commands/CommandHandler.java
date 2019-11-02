package io.th0rgal.oraxen.commands;

import io.th0rgal.oraxen.settings.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.HashMap;

//The class will implement CommandExecutor.
public class CommandHandler implements CommandExecutor {

    //This is where we will store the commands
    private static HashMap<String, CommandInterface> commands = new HashMap<String, CommandInterface>();

    //Register method. When we register commands in our onEnable() we will use this.
    public CommandHandler register(String name, CommandInterface cmd) {

        //When we register the command, this is what actually will put the command in the hashmap.
        commands.put(name, cmd);
        return this;
    }

    //This will be used to check if a string exists or not.
    public boolean exists(String name) {

        //To actually check if the string exists, we will return the hashmap
        return commands.containsKey(name);
    }

    //Getter method for the Executor.
    public CommandInterface getExecutor(String name) {

        //Returns a command in the hashmap of the same name.
        return commands.get(name);
    }

    //This will be a template. All commands will have this in common.
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

        //If there aren't any arguments, what is the command name going to be?
        if (args.length == 0) {
            getExecutor("oraxen").onCommand(sender, cmd, commandLabel, args);
            return true;
        }

        //What if there are arguments in the command? Such as /example args
        if (args.length > 0) {

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

        return false;
    }

}
