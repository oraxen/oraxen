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
    private static final List<String> LOADED_VARIANTS = new ArrayList<>();

    public CustomMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        CustomMechanicListeners customMechanicListeners = new CustomMechanicListeners();
        for (String subMechanicName : section.getKeys(false)) {
            ConfigurationSection subsection = section.getConfigurationSection(subMechanicName);
            CustomMechanicAction action = new CustomMechanicAction(subsection.getStringList("actions"));
            CustomMechanicCondition condition = new CustomMechanicCondition(subsection.getStringList("conditions"));
            if (!LOADED_VARIANTS.contains(section.getName())) {
                LOADED_VARIANTS.add(section.getName());
                customMechanicListeners.registerListener(mechanicFactory, subsection.getString("event"), action, condition);
            }
        }
    }

}
