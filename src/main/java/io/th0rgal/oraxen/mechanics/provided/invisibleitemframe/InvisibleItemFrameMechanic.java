package io.th0rgal.oraxen.mechanics.provided.invisibleitemframe;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.persistence.PersistentDataType;

public class InvisibleItemFrameMechanic extends Mechanic {

    private final boolean isInvisible;
    public static final NamespacedKey invisibleKey = new NamespacedKey(OraxenPlugin.get(), "invisible");

    public InvisibleItemFrameMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section, itemBuilder -> itemBuilder.setCustomTag(invisibleKey, PersistentDataType.BYTE, (byte) 1));
        this.isInvisible = section.getBoolean("isInvisible", false);
    }

    public boolean isInvisible() {
        return isInvisible;
    }
}
