package io.th0rgal.oraxen.mechanics.provided.custom;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;
import java.util.ArrayList;
import java.util.List;

public class CustomMechanic extends Mechanic {

    @SuppressWarnings("unused")
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
