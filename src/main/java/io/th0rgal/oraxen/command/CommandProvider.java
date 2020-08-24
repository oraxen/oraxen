package io.th0rgal.oraxen.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;

import com.syntaxphoenix.syntaxapi.command.CommandManager;
import com.syntaxphoenix.syntaxapi.logging.LoggerState;
import com.syntaxphoenix.syntaxapi.logging.SynLogger;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.command.commands.CommandListener;
import io.th0rgal.oraxen.event.command.OraxenCommandEvent;
import io.th0rgal.oraxen.utils.logs.ConsoleAdapter;

public class CommandProvider {

    private final SynLogger logger = new SynLogger(LoggerState.CUSTOM).setCustom(ConsoleAdapter.INSTANCE);

    private final ArrayList<CommandInfo> infos = new ArrayList<>();
    private final CommandManager manager;

    private int size = 10;

    public CommandProvider(OraxenPlugin plugin) {
        this.manager = new CommandManager().setLogger(logger);
        Bukkit.getPluginManager().registerEvents(new CommandListener(), plugin);
        PluginCommand command = plugin.getCommand("oraxen");
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

    public SynLogger getLogger() {
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
        for (int index = 0; index < infos.length; index++)
            add(infos[index]);
        return this;
    }

    public CommandProvider addAll(Collection<CommandInfo> infos) {
        if (infos != null && !infos.isEmpty())
            for (CommandInfo info : infos)
                add(info);
        return this;
    }

    public CommandProvider removeAll(CommandInfo... infos) {
        for (int index = 0; index < infos.length; index++)
            remove(infos[index]);
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
     * Info getter
     */

    public Optional<CommandInfo> getOptionalInfo(String name) {
        return infos.stream().filter(info -> info.has(name)).findFirst();
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

    /*
     * Shutdown
     */

    public void call(boolean state) {
        if (state) {
            Bukkit.getPluginManager().callEvent(new OraxenCommandEvent(this));
            return;
        }
        manager.unregisterAll();
    }

}
