package io.th0rgal.oraxen.mechanics.provided.commands;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.commands.CommandsParser;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class CommandsMechanic extends Mechanic {

    private CommandsParser commandsParser;

    public CommandsMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);

        this.commandsParser = new CommandsParser(section);
    }

    public CommandsParser getCommands() {
        return commandsParser;
    }

}
