package io.th0rgal.oraxen.mechanics.provided.misc.commands;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.commands.CommandsParser;
import io.th0rgal.oraxen.utils.timers.Timer;
import io.th0rgal.oraxen.utils.timers.TimersFactory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class CommandsMechanic extends Mechanic {

    private final CommandsParser commandsParser;
    private final TimersFactory timersFactory;
    private boolean oneUsage;
    private String permission;

    public CommandsMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);

        commandsParser = new CommandsParser(section, null);

        if (section.isBoolean("one_usage"))
            oneUsage = section.getBoolean("one_usage");

        if (section.isString("permission"))
            permission = section.getString("permission");

        timersFactory = new TimersFactory(section.getLong("cooldown"));

    }

    public boolean isOneUsage() {
        return oneUsage;
    }

    public String getPermission() {
        return permission;
    }

    public boolean hasPermission(Player player) {
        return permission == null || player.hasPermission(permission);
    }

    public Timer getTimer(Player player) {
        return timersFactory.getTimer(player);
    }

    public CommandsParser getCommands() {
        return commandsParser;
    }

}
