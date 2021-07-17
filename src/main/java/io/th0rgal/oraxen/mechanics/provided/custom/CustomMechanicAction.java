package io.th0rgal.oraxen.mechanics.provided.custom;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class CustomMechanicAction {

    private final List<CustomAction> actions = new ArrayList<>();

    public CustomMechanicAction(List<String> eventNames) {
        for (String eventName : eventNames) {
            String[] params = eventName.split(":");

            CustomAction action = switch (params[0]) {
                case "command" -> new CommandAction(params);
                case "message" -> null;
                case "title" -> null;
                default -> null;
            };

            actions.add(action);
        }
    }

    public void run(CustomMechanicWrapper wrapper) {
        for (CustomAction action : actions)
            action.run(wrapper);
    }

}


abstract class CustomAction {

    protected final String[] fields;

    CustomAction(String[] fields) {
        this.fields = fields;
    }

    abstract public void run(CustomMechanicWrapper wrapper);

}

class CommandAction extends CustomAction {

    private final CustomMechanicWrapper.Field commandSender;
    private final boolean op;
    private final String command;

    CommandAction(String[] fields) {
        super(fields);
        commandSender = CustomMechanicWrapper.Field.get(fields[1]);
        op = CustomMechanicWrapper.Field.get(fields[2]) == CustomMechanicWrapper.Field.OP;
        command = fields[3];
    }

    @Override
    public void run(CustomMechanicWrapper wrapper) {
        CommandSender sender = wrapper.getCommandSender(commandSender);
        Bukkit.dispatchCommand(sender, command);
    }
}