package io.th0rgal.oraxen.mechanics.provided.farming.watering;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

public class WateringMechanic extends Mechanic {

    private final boolean isWateringCan;
    private final int uses;
    private final String remainingUsesGlyph;

    public WateringMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        isWateringCan = section.getBoolean("isWaterCan", false);
        uses = section.getInt("uses", 1);
        remainingUsesGlyph = section.getString("remainingUsesGlyph");
    }

    public boolean isWateringCan() { return this.isWateringCan; }

    public int getUses() { return uses; }

    public String getRemainingUseGlyph() { return remainingUsesGlyph; }
}
