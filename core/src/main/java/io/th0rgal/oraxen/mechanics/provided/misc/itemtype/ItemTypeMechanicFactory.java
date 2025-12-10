package io.th0rgal.oraxen.mechanics.provided.misc.itemtype;

import io.th0rgal.oraxen.mechanics.ConfigProperty;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicInfo;
import io.th0rgal.oraxen.mechanics.PropertyType;
import org.bukkit.configuration.ConfigurationSection;

@MechanicInfo(
        category = "misc",
        description = "Defines custom item type behavior for categorization"
)
public class ItemTypeMechanicFactory extends MechanicFactory {

    @ConfigProperty(type = PropertyType.STRING, description = "Custom item type identifier")
    public static final String PROP_TYPE = "type";

    private static ItemTypeMechanicFactory instance;

    public static ItemTypeMechanicFactory get() {
        return instance;
    }

    public ItemTypeMechanicFactory(ConfigurationSection section) {
        super(section);
        instance = this;
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new ItemTypeMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }
}
