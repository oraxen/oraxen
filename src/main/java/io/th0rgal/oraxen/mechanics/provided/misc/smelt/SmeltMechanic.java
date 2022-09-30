package io.th0rgal.oraxen.mechanics.provided.misc.smelt;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

public class SmeltMechanic extends Mechanic {
    private final int burnTime;
    private final boolean hasReplacement;
    private String replacementItemType;
    private String replacementItem;

    public SmeltMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        burnTime = section.getInt("burn_time");
        if (section.isConfigurationSection("replacement")) {
            ConfigurationSection replacementSection = section.getConfigurationSection("replacement");
            hasReplacement = true;
            assert replacementSection != null;
            replacementItemType = replacementSection.getString("item_type");
            replacementItem = replacementSection.getString("item");
        }
        else {
            hasReplacement = false;
        }
    }

    public int getBurnTime() {
        return burnTime;
    }

    public boolean isHasReplacement() {
        return hasReplacement != false;
    }

    public String getReplacementItemType() {
        return replacementItemType;
    }

    public String getReplacementItem() {
        return replacementItem;
    }
}
