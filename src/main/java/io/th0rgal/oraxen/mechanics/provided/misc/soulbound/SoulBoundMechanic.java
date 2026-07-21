package io.th0rgal.oraxen.mechanics.provided.misc.soulbound;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;

public class SoulBoundMechanic extends Mechanic {

    public static final NamespacedKey NAMESPACED_KEY = new NamespacedKey(OraxenPlugin.get(), "soulbound");
    private final double loseChance;

    public SoulBoundMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        this.loseChance = section.getDouble("lose_chance");
    }

    public double getLoseChance() {
        return loseChance;
    }
}
