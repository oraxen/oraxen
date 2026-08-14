package io.th0rgal.oraxen.mechanics.provided.misc.custom;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomEvent;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners.CustomListener;
import io.th0rgal.oraxen.utils.actions.ClickAction;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CustomMechanic extends Mechanic {

    // Mutated during item parsing, which on Folia runs on the reloading
    // command's region thread across reloads.
    private static final Map<String, CustomListener> LOADED_VARIANTS = new ConcurrentHashMap<>();

    public CustomMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);

        for (String subMechanicName : section.getKeys(false)) {
            ConfigurationSection subsection = section.getConfigurationSection(subMechanicName);
            if (subsection == null) continue;
            String key = subsection.getCurrentPath();

            CustomListener loadedListener = LOADED_VARIANTS.get(key);
            if (loadedListener != null) {
                loadedListener.unregister();
            }

            ClickAction clickAction = ClickAction.from(subsection);

            if (clickAction == null) continue;

            CustomListener listener = new CustomEvent(
                    subsection.getString("event", ""),
                    subsection.getBoolean("one_usage", false)
            ).getListener(getItemID(), subsection.getLong("cooldown"), clickAction);

            listener.register();
            LOADED_VARIANTS.put(key, listener);
        }
    }

}
