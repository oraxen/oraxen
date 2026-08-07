package io.th0rgal.oraxen.mechanics.provided.gameplay.repair;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

@Deprecated(forRemoval = true, since = "1.21")
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

    public int getFinalDamage(int maxDurability, int damage) {
        int amountToRepair = (ratio != -1) ? (int) (ratio * maxDurability) : fixedAmount;
        return Math.max(damage - amountToRepair, 0);
    }
}
