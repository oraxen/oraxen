package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class FurnitureFactory extends MechanicFactory {

    public static FurnitureFactory instance;
    public final List<String> toolTypes;

    public FurnitureFactory(ConfigurationSection section) {
        super(section);
        toolTypes = section.getStringList("tool_types");
        MechanicsManager.registerListeners(OraxenPlugin.get(), new FurnitureListener(this));
        instance = this;
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new FurnitureMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }

    public static FurnitureFactory getInstance() {
        return instance;
    }
}
