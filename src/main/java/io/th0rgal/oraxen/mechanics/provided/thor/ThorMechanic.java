package io.th0rgal.oraxen.mechanics.provided.thor;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Random;

public class ThorMechanic extends Mechanic {

    private int lightningBoltsAmount;
    private double randomLocationVariation;

    public ThorMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        this.lightningBoltsAmount = section.getInt("lightning_bolts_amount");
        this.randomLocationVariation = section.getDouble("random_location_variation");
    }

    public int getLightningBoltsAmount() {
        return lightningBoltsAmount;
    }

    public Location getRandomizedLocation(Location location) {
        Random random = new Random();
        location.setX(location.getX() + (random.nextDouble() * randomLocationVariation) - randomLocationVariation / 2);
        location.setY(location.getY() + (random.nextDouble() * randomLocationVariation) - randomLocationVariation / 2);
        return location;
    }
}
