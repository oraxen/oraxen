package io.th0rgal.oraxen.mechanics.provided.furniture;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.NamespacedKey;
import org.bukkit.Rotation;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.persistence.PersistentDataType;

public class FurnitureMechanic extends Mechanic {

    private final boolean barrier;
    private final Rotation rotation;
    private final BlockFace facing;
    public static final NamespacedKey FURNITURE_KEY = new NamespacedKey(OraxenPlugin.get(), "furniture");

    public FurnitureMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section, itemBuilder -> itemBuilder.setCustomTag(FURNITURE_KEY,
                PersistentDataType.BYTE, (byte) 1));
        this.barrier = section.getBoolean("barrier", false);
        this.rotation = Rotation.valueOf(section.getString("rotation", "NONE").toUpperCase());
        this.facing = BlockFace.valueOf(section.getString("facing", "UP").toUpperCase());
    }

    public boolean hasBarrier() {
        return barrier;
    }

    public Rotation getRotation() {
        return rotation;
    }

    public BlockFace getFacing() {
        return facing;
    }
}
