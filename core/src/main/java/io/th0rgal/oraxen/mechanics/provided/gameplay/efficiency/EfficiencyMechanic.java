package io.th0rgal.oraxen.mechanics.provided.gameplay.efficiency;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.wrappers.PotionEffectTypeWrapper;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffectType;

public class EfficiencyMechanic extends Mechanic {

    private final int amount;
    private final PotionEffectType type;

    public EfficiencyMechanic(final MechanicFactory mechanicFactory, final ConfigurationSection section) {
        super(mechanicFactory, section);
        final int tempAmount = section.getInt("amount", 1);
        if (tempAmount < 0) {
            type = PotionEffectTypeWrapper.MINING_FATIGUE;
            amount = -tempAmount;
        } else {
            type = PotionEffectTypeWrapper.HASTE;
            amount = tempAmount;
        }
    }

    public int getAmount() {
        return amount;
    }

    public PotionEffectType getType() {
        return type;
    }
}
