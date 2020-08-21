package io.th0rgal.oraxen.command;

import static com.syntaxphoenix.syntaxapi.command.DefaultArgumentSerializer.DEFAULT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import com.syntaxphoenix.syntaxapi.command.Arguments;
import com.syntaxphoenix.syntaxapi.command.BaseCommand;
import com.syntaxphoenix.syntaxapi.command.CommandManager;
import com.syntaxphoenix.syntaxapi.command.CommandProcess;
import com.syntaxphoenix.syntaxapi.command.ExecutionState;

import io.th0rgal.oraxen.language.Message;
import io.th0rgal.oraxen.utils.general.Placeholder;

public class CommandRedirect implements CommandExecutor, TabCompleter {

    private final CommandProvider provider;
    private final CommandManager manager;

    public CommandRedirect(CommandProvider provider) {
        this.manager = (this.provider = provider).getManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command ignore, String alias, String[] args) {

        CommandProcess process = prepare(sender, args.length == 0 ? new String[] { "help" } : args);

        if (!process.isValid() || process.getCommand() == null) {
            Message.COMMAND_NOT_EXIST.send(sender, Placeholder.of("name", process.getLabel()));
            return true;
        } 

        return process.execute(manager) == ExecutionState.SUCCESS;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command ignore, String alias, String[] args) {

        ArrayList<String> output = new ArrayList<>();

        CommandProcess process = prepare(sender, args);

        if (!process.isValid() || process.getCommand() == null) {
            provider.getInfos().stream().map(info -> info.getName()).forEach(value -> output.add(value));
            return output;
        }

        BaseCommand command = process.getCommand();
        if (!(command instanceof OraxenCommand)) {
            provider.getInfos().stream().map(info -> info.getName()).forEach(value -> output.add(value));
            return output;
        }
        
        System.out.println(Arrays.asList(args));
        
        MinecraftInfo info = (MinecraftInfo) process.constructInfo();
        Arguments arguments = process.getArguments();
        
        System.out.println(arguments.toString());

        Arguments completion = ((OraxenCommand) command).complete(info, arguments).getCompletion();

        int size = completion.count();
        for (int index = 1; index <= size; index++)
            output.add(DEFAULT.toString(completion.get(index)));

        return output;
    }

    private CommandProcess prepare(CommandSender sender, String[] arguments) {
        CommandProcess process = manager.process(arguments);
        process.setInfoConstructor((manager, label) -> new MinecraftInfo(manager, label, provider.getInfo(label), sender));
        return process;
    }

}
