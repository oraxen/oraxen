package io.th0rgal.oraxen.mechanics.provided.thor;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Random;

public class ThorMechanic extends Mechanic {

    private long nextAllowedUsageTime = 0;
    private int lightningBoltsAmount;
    private double randomLocationVariation;
    private int delay;

    public ThorMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        this.lightningBoltsAmount = section.getInt("lightning_bolts_amount");
        this.randomLocationVariation = section.getDouble("random_location_variation");
        this.delay = section.getInt("delay");
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

    public long getRemainingTime() { // returns the remaining time before next usage
        long remainingTime = nextAllowedUsageTime - System.currentTimeMillis();
        if (remainingTime > 0)
            return remainingTime;
        nextAllowedUsageTime = System.currentTimeMillis() + delay;
        return remainingTime;
    }
}
