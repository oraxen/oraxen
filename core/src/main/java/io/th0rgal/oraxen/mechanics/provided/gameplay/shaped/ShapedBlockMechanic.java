package io.th0rgal.oraxen.mechanics.provided.gameplay.shaped;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.light.LightMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.limitedplacing.LimitedPlacing;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.oraxen.utils.drops.Loot;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Mechanic for custom shaped block variants (stairs, slabs, doors, trapdoors, grates).
 * Uses waxed copper blocks as the base to prevent oxidation while allowing custom models.
 */
public class ShapedBlockMechanic extends Mechanic {

    public static final NamespacedKey SHAPED_BLOCK_KEY = new NamespacedKey(OraxenPlugin.get(), "shaped_block");

    private final ShapedBlockType blockType;
    private final int customVariation;
    private final Material placedMaterial;
    private final Drop drop;
    private final int hardness;
    private final LightMechanic light;
    private final LimitedPlacing limitedPlacing;
    private final BlockSounds blockSounds;
    private String model;

    @SuppressWarnings("unchecked")
    public ShapedBlockMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);

        // Parse block type
        String typeStr = section.getString("type", "STAIR").toUpperCase();
        try {
            this.blockType = ShapedBlockType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid shaped block type: " + typeStr +
                ". Valid types: STAIR, SLAB, DOOR, TRAPDOOR, GRATE, BULB");
        }

        // Check if this block type is available in the current Minecraft version
        if (!blockType.isAvailable()) {
            throw new UnsupportedOperationException("Block type " + typeStr + 
                " is not available in this Minecraft version. This block type requires a newer version of Minecraft.");
        }

        // Parse custom variation (1-4)
        this.customVariation = section.getInt("custom_variation", 1);
        if (customVariation < 1 || customVariation > 4) {
            throw new IllegalArgumentException("custom_variation must be between 1 and 4 for shaped blocks");
        }

        // Get the actual material to place
        this.placedMaterial = blockType.getMaterial(customVariation);

        // Parse model
        this.model = section.getString("model");

        // Parse hardness
        this.hardness = section.getInt("hardness", 3);

        // Parse light
        this.light = new LightMechanic(section);

        // Parse drop configuration
        ConfigurationSection dropSection = section.getConfigurationSection("drop");
        if (dropSection != null) {
            List<Loot> loots = new ArrayList<>();
            for (LinkedHashMap<String, Object> lootConfig : (List<LinkedHashMap<String, Object>>) dropSection.getList("loots", new ArrayList<>())) {
                loots.add(new Loot(lootConfig, getItemID()));
            }
            ShapedBlockMechanicFactory factory = (ShapedBlockMechanicFactory) mechanicFactory;
            if (dropSection.isString("minimal_type")) {
                this.drop = new Drop(factory.getToolTypes(), loots,
                    dropSection.getBoolean("silktouch"),
                    dropSection.getBoolean("fortune"),
                    getItemID(),
                    dropSection.getString("minimal_type"),
                    new ArrayList<>());
            } else {
                this.drop = new Drop(loots, dropSection.getBoolean("silktouch"),
                    dropSection.getBoolean("fortune"), getItemID());
            }
        } else {
            this.drop = new Drop(new ArrayList<>(), false, false, getItemID());
        }

        // Parse limited placing
        ConfigurationSection limitedPlacingSection = section.getConfigurationSection("limited_placing");
        this.limitedPlacing = limitedPlacingSection != null ? new LimitedPlacing(limitedPlacingSection) : null;

        // Parse block sounds
        ConfigurationSection blockSoundsSection = section.getConfigurationSection("block_sounds");
        this.blockSounds = blockSoundsSection != null ? new BlockSounds(blockSoundsSection) : null;
    }

    public ShapedBlockType getBlockType() {
        return blockType;
    }

    public int getCustomVariation() {
        return customVariation;
    }

    public Material getPlacedMaterial() {
        return placedMaterial;
    }

    public String getModel(ConfigurationSection section) {
        if (model != null) return model;
        // Try to get explicit model from Pack config
        String packModel = section.getString("Pack.model");
        if (packModel != null) return packModel;
        // Fall back to item ID as model name (used when generate_model: true)
        return getItemID();
    }

    public Drop getDrop() {
        return drop;
    }

    public int getHardness() {
        return hardness;
    }

    public boolean hasHardness() {
        return hardness != -1;
    }

    public LightMechanic getLight() {
        return light;
    }

    public boolean hasLight() {
        return light.hasLightLevel();
    }

    public boolean hasLimitedPlacing() {
        return limitedPlacing != null;
    }

    public LimitedPlacing getLimitedPlacing() {
        return limitedPlacing;
    }

    public boolean hasBlockSounds() {
        return blockSounds != null;
    }

    public BlockSounds getBlockSounds() {
        return blockSounds;
    }
}
