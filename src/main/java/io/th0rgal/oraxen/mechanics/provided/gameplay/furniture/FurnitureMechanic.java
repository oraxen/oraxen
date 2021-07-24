package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.oraxen.utils.drops.Loot;
import org.bukkit.NamespacedKey;
import org.bukkit.Rotation;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class FurnitureMechanic extends Mechanic {

    private final boolean barrier;
    private final boolean hasRotation;
    private Rotation rotation;
    private final boolean hasSeat;
    private float seatHeight;
    private float seatYaw;
    private final BlockFace facing;
    public static final NamespacedKey FURNITURE_KEY = new NamespacedKey(OraxenPlugin.get(), "furniture");
    public static final NamespacedKey SEAT_KEY = new NamespacedKey(OraxenPlugin.get(), "seat");
    private final Drop drop;

    @SuppressWarnings("unchecked")
    public FurnitureMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section, itemBuilder -> itemBuilder.setCustomTag(FURNITURE_KEY,
                PersistentDataType.BYTE, (byte) 1));
        this.barrier = OraxenPlugin.getProtocolLib() && section.getBoolean("barrier", false);
        if (section.isString("rotation")) {
            this.rotation = Rotation.valueOf(section.getString("rotation", "NONE").toUpperCase());
            hasRotation = true;
        } else
            hasRotation = false;

        if (section.isConfigurationSection("seat")) {
            ConfigurationSection seatSection = section.getConfigurationSection("seat");
            hasSeat = true;
            seatHeight = (float) seatSection.getDouble("height");
            seatYaw = (float) seatSection.getDouble("yaw");
        } else
            hasSeat = false;

        this.facing = BlockFace.valueOf(section.getString("facing", "UP").toUpperCase());

        List<Loot> loots = new ArrayList<>();
        if (section.isConfigurationSection("drop")) {
            ConfigurationSection drop = section.getConfigurationSection("drop");
            for (LinkedHashMap<String, Object> lootConfig : (List<LinkedHashMap<String, Object>>)
                    drop.getList("loots"))
                loots.add(new Loot(lootConfig));

            if (drop.isString("minimal_type")) {
                FurnitureFactory mechanic = (FurnitureFactory) mechanicFactory;
                List<String> bestTools = drop.isList("best_tools")
                        ? drop.getStringList("best_tools")
                        : new ArrayList<>();
                this.drop = new Drop(mechanic.toolTypes, loots, drop.getBoolean("silktouch"),
                        drop.getBoolean("fortune"), getItemID(),
                        drop.getString("minimal_type"),
                        bestTools);
            } else
                this.drop = new Drop(loots, drop.getBoolean("silktouch"), drop.getBoolean("fortune"),
                        getItemID());
        } else
            this.drop = new Drop(loots, false, false, getItemID());

    }

    public boolean hasBarrier() {
        return barrier;
    }

    public boolean hasRotation() {
        return hasRotation;
    }

    public Rotation getRotation() {
        return rotation;
    }

    public boolean hasSeat() {
        return hasSeat;
    }

    public float getSeatHeight() {
        return seatHeight;
    }

    public float getSeatYaw() {
        return seatYaw;
    }

    public BlockFace getFacing() {
        return facing;
    }

    public Drop getDrop() {
        return drop;
    }
}
