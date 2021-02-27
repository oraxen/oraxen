package org.playuniverse.snowypine.command;

import static com.syntaxphoenix.syntaxapi.command.DefaultArgumentSerializer.DEFAULT;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.playuniverse.snowypine.command.argument.function.FunctionHelper;
import org.playuniverse.snowypine.command.permission.SnowypinePermissions;
import org.playuniverse.snowypine.language.Message;
import org.playuniverse.snowypine.utils.general.Placeholder;

import com.syntaxphoenix.syntaxapi.command.Arguments;
import com.syntaxphoenix.syntaxapi.command.BaseCommand;
import com.syntaxphoenix.syntaxapi.command.CommandManager;
import com.syntaxphoenix.syntaxapi.command.CommandProcess;
import com.syntaxphoenix.syntaxapi.command.ExecutionState;

public class CommandRedirect implements CommandExecutor, TabCompleter {

	public static final Pattern TAG_PATTERN = Pattern.compile("(<\\S*>)");

	private final CommandProvider provider;
	private final CommandManager manager;

	public CommandRedirect(CommandProvider provider) {
		this.manager = (this.provider = provider).getManager();
	}

	@Override
	public boolean onCommand(CommandSender sender, org.bukkit.command.Command ignore, String alias, String[] args) {

		CommandProcess process = prepare(sender, args.length == 0 ? new String[] {
				"help"
		} : args);

		if (!process.isValid() || process.getCommand() == null) {
			Message.COMMAND_NOT_EXIST.send(sender, Placeholder.of("name", process.getLabel()));
			return true;
		}

		CommandInfo info = provider.getInfo(process.getLabel());
		if (info == null) {
			Message.COMMAND_NOT_EXIST.send(sender, Placeholder.of("name", process.getLabel()));
			return true;
		}
		if (!SnowypinePermissions.USE.required(sender)) {
			return true;
		}
		if (info.getPermission() != null && !info.getPermission().required(sender)) {
			return true;
		}
		return process.execute(manager) == ExecutionState.SUCCESS;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command ignore, String alias, String[] args) {

		ArrayList<String> output = new ArrayList<>();

		if (!SnowypinePermissions.USE.has(sender)) {
			return null;
		}

		CommandProcess process = prepare(sender, args);

		String label = process.isValid() ? process.getLabel() : "";

		if (!process.isValid() || process.getCommand() == null) {
			getPermittedCommands(sender).map(CommandInfo::getName).filter(value -> matches(label, value)).forEach(output::add);
			return output;
		}

		BaseCommand command = process.getCommand();
		if (!(command instanceof Command)) {
			getPermittedCommands(sender).map(CommandInfo::getName).filter(value -> matches(label, value)).forEach(output::add);
			return output;
		}

		Arguments arguments = process.getArguments();
		String argument = FunctionHelper.of(() -> DEFAULT.toString(arguments.get(arguments.count())), "").get();

		MinecraftInfo info = (MinecraftInfo) process.constructInfo();

		Arguments completion = ((Command) command).complete(info, arguments).getCompletion();

		int size = completion.count();
		for (int index = 1; index <= size; index++) {
			String value = DEFAULT.toString(completion.get(index));
			if (matches(argument, value))
				output.add(value);
		}

		return output;
	}

	public Stream<CommandInfo> getPermittedCommands(CommandSender sender) {
		return provider.getInfos().stream().filter(info -> info.getPermission() == null ? true : info.getPermission().has(sender));
	}

	private boolean matches(String current, String value) {
		return current.isEmpty() || value.contains(current) || TAG_PATTERN.matcher(value).find();
	}

	private CommandProcess prepare(CommandSender sender, String[] arguments) {
		CommandProcess process = manager.process(arguments);
		process.setInfoConstructor((manager, label) -> new MinecraftInfo(provider, manager, label, provider.getInfo(label), sender));
		return process;
	}

}
