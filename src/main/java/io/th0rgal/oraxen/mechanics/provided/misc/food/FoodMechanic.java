package io.th0rgal.oraxen.mechanics.provided.misc.food;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

public class FoodMechanic extends Mechanic {

    public final int hunger;
    public final int saturation;

    public FoodMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        hunger = section.getInt("hunger");
        saturation = section.getInt("saturation");
    }

    public int getHunger() {
        return hunger;
    }

    public int getSaturation() {
        return saturation;
    }
}
