package io.th0rgal.oraxen.mechanics.provided.bedrockbreak;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;

import org.bukkit.configuration.ConfigurationSection;

public class BedrockBreakMechanicFactory extends MechanicFactory {

    private boolean disabledOnFirstLayer;
    private int durabilityCost;

    public BedrockBreakMechanicFactory(ConfigurationSection section) {
        super(section);
        disabledOnFirstLayer = section.getBoolean("disable_on_first_layer");
        durabilityCost = section.getInt("durability_cost");
        new BedrockBreakMechanicsManager(this);
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new BedrockBreakMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }

    public boolean isDisabledOnFirstLayer() {
        return disabledOnFirstLayer;
    }

    public int getDurabilityCost() {
        return durabilityCost;
    }
}