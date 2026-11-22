package io.th0rgal.oraxen.mechanics.provided.combat.spell.thor;

import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.combat.spell.SpellMechanic;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class ThorMechanic extends SpellMechanic {

    private final int lightningBoltsAmount;
    private final double randomLocationVariation;

    public ThorMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        this.lightningBoltsAmount = section.getInt("lightning_bolts_amount");
        this.randomLocationVariation = section.getDouble("random_location_variation");
    }

    public int getLightningBoltsAmount() {
        return lightningBoltsAmount;
    }

    public Location getRandomizedLocation(Location location) {
        Random random = ThreadLocalRandom.current();
        location.setX(location.getX() + (random.nextDouble() * randomLocationVariation) - randomLocationVariation / 2);
        location.setY(location.getY() + (random.nextDouble() * randomLocationVariation) - randomLocationVariation / 2);
        return location;
    }
}
