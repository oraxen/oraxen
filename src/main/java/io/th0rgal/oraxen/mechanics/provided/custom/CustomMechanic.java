package io.th0rgal.oraxen.mechanics.provided.custom;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.commands.CommandsParser;
import io.th0rgal.oraxen.utils.timers.Timer;
import io.th0rgal.oraxen.utils.timers.TimersFactory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CustomMechanic extends Mechanic {

    private boolean oneUsage;

    public CustomMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        CustomMechanicListeners customMechanicListeners = new CustomMechanicListeners((CustomMechanicFactory) mechanicFactory);
        for (String subMechanicName : section.getKeys(false)) {
            CustomMechanicAction action = new CustomMechanicAction(section.getStringList("actions"));
            CustomMechanicCondition condition = new CustomMechanicCondition(section.getStringList("conditions"));
            customMechanicListeners.registerListener(section.getString("event"), action, condition);
        }
    }

}
