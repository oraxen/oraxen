package io.th0rgal.oraxen.mechanics.provided.gameplay.efficiency;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.PotionUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffectType;

public class EfficiencyMechanic extends Mechanic {

    private static final double HASTE_SPEED_PER_LEVEL = 0.2D;

    private final int amount;
    private final boolean slowdown;
    private final PotionEffectType type;

    public EfficiencyMechanic(final MechanicFactory mechanicFactory, final ConfigurationSection section) {
        super(mechanicFactory, section);
        final int tempAmount = section.getInt("amount", 1);
        slowdown = tempAmount < 0;
        if (slowdown) {
            type = PotionUtils.getEffectType("mining_fatigue");
            amount = -tempAmount;
        } else {
            type = PotionUtils.getEffectType("haste");
            amount = tempAmount;
        }
    }

    public int getAmount() {
        return amount;
    }

    public PotionEffectType getType() {
        return type;
    }

    /**
     * Mining speed multiplier this mechanic grants, where {@code 1.0} is vanilla speed. It mirrors
     * the vanilla effects the mechanic is configured after: haste speeds mining up by 20% per
     * level, mining fatigue uses vanilla's discrete per-level multipliers.
     */
    public double getMiningSpeedMultiplier() {
        if (!slowdown) return 1.0D + HASTE_SPEED_PER_LEVEL * amount;
        return switch (amount) {
            case 1 -> 0.3D;
            case 2 -> 0.09D;
            case 3 -> 0.0027D;
            default -> 0.00081D;
        };
    }
}
