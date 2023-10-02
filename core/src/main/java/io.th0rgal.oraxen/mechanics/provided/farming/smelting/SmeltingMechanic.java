package io.th0rgal.oraxen.mechanics.provided.farming.smelting;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

public class SmeltingMechanic extends Mechanic {

    private final boolean playSound;

    public SmeltingMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        playSound = section.getBoolean("play_sound");
    }

    public boolean playSound() {
        return this.playSound;
    }

}