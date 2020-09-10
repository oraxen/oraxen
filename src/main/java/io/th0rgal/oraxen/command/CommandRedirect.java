package io.th0rgal.oraxen.command;

import static com.syntaxphoenix.syntaxapi.command.DefaultArgumentSerializer.DEFAULT;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import com.syntaxphoenix.syntaxapi.command.Arguments;
import com.syntaxphoenix.syntaxapi.command.BaseCommand;
import com.syntaxphoenix.syntaxapi.command.CommandManager;
import com.syntaxphoenix.syntaxapi.command.CommandProcess;
import com.syntaxphoenix.syntaxapi.command.ExecutionState;

import io.th0rgal.oraxen.command.argument.function.FunctionHelper;
import io.th0rgal.oraxen.language.Message;
import io.th0rgal.oraxen.utils.general.Placeholder;

public class CommandRedirect implements CommandExecutor, TabCompleter {

    public static final Pattern TAG_PATTERN = Pattern.compile("(<\\S*>)");

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

        String label = process.isValid() ? process.getLabel() : "";

        if (!process.isValid() || process.getCommand() == null) {
            provider
                .getInfos()
                .stream()
                .map(CommandInfo::getName)
                .filter(value -> matches(label, value))
                .forEach(output::add);
            return output;
        }

        BaseCommand command = process.getCommand();
        if (!(command instanceof OraxenCommand)) {
            provider
                .getInfos()
                .stream()
                .map(CommandInfo::getName)
                .filter(value -> matches(label, value))
                .forEach(output::add);
            return output;
        }

        Arguments arguments = process.getArguments();
        String argument = FunctionHelper.of(() -> DEFAULT.toString(arguments.get(arguments.count())), "").get();

        MinecraftInfo info = (MinecraftInfo) process.constructInfo();

        Arguments completion = ((OraxenCommand) command).complete(info, arguments).getCompletion();

        int size = completion.count();
        for (int index = 1; index <= size; index++) {
            String value = DEFAULT.toString(completion.get(index));
            if (matches(argument, value))
                output.add(value);
        }

        return output;
    }

    private boolean matches(String current, String value) {
        return current.isEmpty() || value.contains(current) || TAG_PATTERN.matcher(value).find();
    }

    private CommandProcess prepare(CommandSender sender, String[] arguments) {
        CommandProcess process = manager.process(arguments);
        process
            .setInfoConstructor((manager, label) -> new MinecraftInfo(manager, label, provider.getInfo(label), sender));
        return process;
    }

}
