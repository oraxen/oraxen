package io.th0rgal.oraxen.mechanics.provided.misc.custom;

import io.th0rgal.oraxen.mechanics.ConfigProperty;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicInfo;
import io.th0rgal.oraxen.mechanics.PropertyType;
import org.bukkit.configuration.ConfigurationSection;

@MechanicInfo(
        category = "misc",
        description = "Custom mechanic with configurable actions and conditions"
)
public class CustomMechanicFactory extends MechanicFactory {

    @ConfigProperty(type = PropertyType.LIST, description = "List of event handlers")
    public static final String PROP_EVENTS = "events";

    public CustomMechanicFactory(ConfigurationSection section) {
        super(section);
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new CustomMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }
}
