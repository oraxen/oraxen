package io.th0rgal.oraxen.mechanics.provided.misc.custom;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomAction;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomCondition;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomEvent;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners.CustomListener;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomMechanic extends Mechanic {

    private static final Map<String, CustomListener> LOADED_VARIANTS = new HashMap<>();

    public CustomMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);

        for (String subMechanicName : section.getKeys(false)) {
            ConfigurationSection subsection = section.getConfigurationSection(subMechanicName);

            String key = subsection.getCurrentPath();
            if (LOADED_VARIANTS.containsKey(key)) LOADED_VARIANTS.get(key).unregister();

            List<CustomAction> actions = new ArrayList<>();
            for (String action : subsection.getStringList("actions")) actions.add(new CustomAction(action));

            List<CustomCondition> conditions = new ArrayList<>();
            for (String condition : subsection.getStringList("conditions"))
                conditions.add(new CustomCondition(condition));

            CustomListener listener = new CustomEvent(subsection.getString("event"),
                    subsection.getBoolean("one_usage", false))
                    .getListener(getItemID(), conditions, actions);

            listener.register();
            LOADED_VARIANTS.put(key, listener);
        }
    }

}
