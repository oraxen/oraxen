package io.th0rgal.oraxen.mechanics.provided.farming.bottledexp;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

public class BottledExpMechanic extends Mechanic {

    final double ratio;

    public BottledExpMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        this.ratio = section.getDouble("ratio");
    }

    private int levelToExp(int level) {
        if (level <= 15)
            return level * level + 6 * level;

        if (level <= 30)
            return (int) (2.5 * level * level - 40.5 * level + 360.0);

        return (int) (4.5 * level * level - 162.5 * level + 2220.0);
    }

    public int getBottleEquivalent(int level, float xp) {
        return (int) Math.ceil((xp + levelToExp(level)) * ratio / 10.0f);
    }

}