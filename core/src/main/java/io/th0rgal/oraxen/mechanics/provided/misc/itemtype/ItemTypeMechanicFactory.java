package io.th0rgal.oraxen.mechanics.provided.misc.itemtype;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

public class ItemTypeMechanicFactory extends MechanicFactory {

    private static ItemTypeMechanicFactory instance;
    public static ItemTypeMechanicFactory get() {
        return instance;
    }

    public ItemTypeMechanicFactory(ConfigurationSection section) {
        super(section);
        instance = this;
    }

    @Override
    public Mechanic parse(ConfigurationSection section) {
        Mechanic mechanic = new ItemTypeMechanic(this, section);
        addToImplemented(mechanic);
        return mechanic;
    }

}
