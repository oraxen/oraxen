package io.th0rgal.oraxen.mechanics.provided.commands;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.commands.CommandsParser;
import io.th0rgal.oraxen.utils.timers.Timer;
import io.th0rgal.oraxen.utils.timers.TimersFactory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class CommandsMechanic extends Mechanic {

    private final CommandsParser commandsParser;
    private boolean oneUsage;
    private String permission;
    private final TimersFactory timersFactory;

    public CommandsMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);

        this.commandsParser = new CommandsParser(section);

        if (section.isBoolean("one_usage"))
            this.oneUsage = section.getBoolean("one_usage");

        if (section.isString("permission"))
            this.permission = section.getString("permission");

        this.timersFactory = new TimersFactory(section.getLong("cooldown"));

    }

    public boolean isOneUsage() {
        return this.oneUsage;
    }

    public String getPermission() {
        return this.permission;
    }

    public boolean hasPermission(Player player) {
        return permission == null || player.hasPermission(this.permission);
    }

    public Timer getTimer(Player player) {
        return timersFactory.getTimer(player);
    }

    public CommandsParser getCommands() {
        return commandsParser;
    }

}
