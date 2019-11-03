package io.th0rgal.oraxen.mechanics.provided.commands;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class CommandsMechanic extends Mechanic {

    private List<String> consoleCommands;
    private List<String> playerCommands;
    private List<String> oppedPlayerCommands;

    public CommandsMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);

        if (section.isList("console"))
            this.consoleCommands = section.getStringList("console");

        if (section.isList("player"))
            this.playerCommands = section.getStringList("player");

        if (section.isList("opped_player"))
            this.oppedPlayerCommands = section.getStringList("opped_player");
    }

    public boolean hasConsoleCommands() {
        return consoleCommands != null;
    }

    public List<String> getConsoleCommands() {
        return consoleCommands;
    }

    public boolean hasPlayerCommands() {
        return playerCommands != null;
    }

    public List<String> getPlayerCommands() {
        return playerCommands;
    }

    public boolean hasOppedPlayerCommands() {
        return oppedPlayerCommands != null;
    }

    public List<String> getOppedPlayerCommands() {
        return oppedPlayerCommands;
    }
}
