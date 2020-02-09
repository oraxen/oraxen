package io.th0rgal.oraxen.mechanics.provided.repair;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

public class RepairMechanic extends Mechanic {

    private double ratio = -1;
    private int fixedAmount = -1;

    public RepairMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        if (section.isDouble("ratio"))
            this.ratio = section.getDouble("ratio");
        if (section.isInt("fixed_amount"))
            this.fixedAmount = section.getInt("fixed_amount");
    }

    public boolean hasRatio() {
        return ratio != -1;
    }

    public double getRatio() {
        return ratio;
    }

    public int getFixedAmount() {
        return fixedAmount;
    }
}
