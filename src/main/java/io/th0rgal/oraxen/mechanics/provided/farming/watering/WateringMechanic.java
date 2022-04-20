package io.th0rgal.oraxen.mechanics.provided.farming.watering;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

public class WateringMechanic extends Mechanic {

    private final boolean isWateringCan;

    public WateringMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        isWateringCan = section.getBoolean("isWaterCan");
    }

    public boolean isWateringCan() { return this.isWateringCan; }
}
