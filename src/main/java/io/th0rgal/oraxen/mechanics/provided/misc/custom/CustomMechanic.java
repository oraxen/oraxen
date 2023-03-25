package io.th0rgal.oraxen.mechanics.provided.misc.custom;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomEvent;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners.CustomListener;
import io.th0rgal.oraxen.utils.actions.ClickAction;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

public class CustomMechanic extends Mechanic {

    private static final Map<String, CustomListener> LOADED_VARIANTS = new HashMap<>();

    public CustomMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);

        for (String subMechanicName : section.getKeys(false)) {
            ConfigurationSection subsection = section.getConfigurationSection(subMechanicName);

            String key = subsection.getCurrentPath();

            CustomListener loadedListener = LOADED_VARIANTS.get(key);
            if (loadedListener != null) {
                loadedListener.unregister();
            }

            boolean cancelEvent = subsection.getBoolean("cancel_event", false);

            ClickAction clickAction = ClickAction.from(subsection, cancelEvent);

            if (clickAction == null) {
                continue;
            }

            CustomListener listener = new CustomEvent(subsection.getString("event"),
                    subsection.getBoolean("one_usage", false), cancelEvent)
                    .getListener(getItemID(), subsection.getLong("cooldown"), clickAction);

            listener.register();
            LOADED_VARIANTS.put(key, listener);
        }
    }

}
