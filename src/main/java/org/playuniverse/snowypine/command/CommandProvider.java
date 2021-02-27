package org.playuniverse.snowypine.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.bukkit.command.PluginCommand;
import org.playuniverse.snowypine.ModuledPlugin;

import com.syntaxphoenix.syntaxapi.command.CommandManager;
import com.syntaxphoenix.syntaxapi.logging.ILogger;

public class CommandProvider {

	private final ILogger logger;

	private final ArrayList<CommandInfo> infos = new ArrayList<>();
	private final CommandManager manager;

	private int size = 10;

	public CommandProvider(ModuledPlugin plugin) {
		this.logger = plugin.getPluginLogger();
		this.manager = new CommandManager().setLogger(logger);
		PluginCommand command = plugin.getCommand("snowypine");
		CommandRedirect redirect = new CommandRedirect(this);
		command.setExecutor(redirect);
		command.setTabCompleter(redirect);
	}

	/*
	 * Getter
	 */

	public CommandManager getManager() {
		return manager;
	}

	public ILogger getLogger() {
		return logger;
	}

	/*
	 * Page management
	 */

	public CommandProvider setPageSize(int size) {
		this.size = size;
		return this;
	}

	public int getPageSize() {
		return size;
	}

	public int getPageCount() {
		return (int) Math.ceil((double) infos.size() / 10);
	}

	/*
	 * Info management
	 */

	public CommandProvider clear() {
		infos.clear();
		return this;
	}

	public CommandProvider add(CommandInfo info) {
		if (!infos.contains(info))
			write(info);
		return this;
	}

	public CommandProvider remove(CommandInfo info) {
		delete(info);
		return this;
	}

	public CommandProvider addAll(CommandInfo... infos) {
		for (CommandInfo info : infos)
			add(info);
		return this;
	}

	public CommandProvider addAll(Collection<CommandInfo> infos) {
		if (infos != null && !infos.isEmpty())
			for (CommandInfo info : infos)
				add(info);
		return this;
	}

	public CommandProvider removeAll(CommandInfo... infos) {
		for (CommandInfo info : infos)
			remove(info);
		return this;
	}

	public CommandProvider removeAll(Collection<CommandInfo> infos) {
		if (infos != null && !infos.isEmpty())
			for (CommandInfo info : infos)
				remove(info);
		return this;
	}

	/*
	 * Intern write to list
	 */

	private void write(CommandInfo info) {
		infos.add(info);
	}

	private void delete(CommandInfo info) {
		infos.remove(info);
	}

	/*
	 * Outer registration
	 */

	public boolean register(CommandInfo info) {
		if (info.register(manager)) {
			add(info);
			return true;
		}
		return false;
	}

	/*
	 * Info getter
	 */

	public Optional<CommandInfo> getOptionalInfo(String name) {
		return infos.stream().filter(info -> info.has(name)).findFirst();
	}

	public Optional<CommandInfo> getOptionalInfo(Command command) {
		return infos.stream().filter(info -> info.getCommand() == command).findFirst();
	}

	public CommandInfo getInfo(String name) {
		return getOptionalInfo(name).orElse(null);
	}

	public List<CommandInfo> getInfos(int page) {
		ArrayList<CommandInfo> list = new ArrayList<>();

		int end = page * size;
		int start = end - size;

		int size = infos.size();

		for (int index = start; index < end; index++) {
			if (index == size)
				break;
			list.add(infos.get(index));
		}

		return list;
	}

	@SuppressWarnings("unchecked")
	public List<CommandInfo> getInfos() {
		return (List<CommandInfo>) infos.clone();
	}

}
