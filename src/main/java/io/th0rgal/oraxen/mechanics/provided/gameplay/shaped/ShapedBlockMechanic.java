package io.th0rgal.oraxen.mechanics.provided.gameplay.shaped;

import com.jeff_media.customblockdata.CustomBlockData;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockBreaking;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockEvents;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.Placeable;
import io.th0rgal.oraxen.mechanics.provided.gameplay.light.LightMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.limitedplacing.LimitedPlacing;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.oraxen.utils.drops.Loot;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;


/**
 * Mechanic for custom shaped block variants (stairs, slabs, doors, trapdoors, grates).
 * Uses waxed copper blocks as the base to prevent oxidation while allowing custom models.
 */
public class ShapedBlockMechanic extends Mechanic {

    public static final NamespacedKey SHAPED_BLOCK_KEY = new NamespacedKey(OraxenPlugin.get(), "block");
    private static final NamespacedKey LEGACY_SHAPED_BLOCK_KEY = new NamespacedKey(OraxenPlugin.get(), "shaped_block");

    private final ShapedBlockType blockType;
    private final int customVariation;
    private final Material placedMaterial;
    private final BlockBreaking breaking;
    private final Placeable placeable;
    private final LightMechanic light;
    private final LimitedPlacing limitedPlacing;
    private final BlockSounds blockSounds;
    private final BlockEvents blockEvents;
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

        // Parse custom variation (1-4)
        this.customVariation = section.getInt("custom_variation", 1);
        if (customVariation < 1 || customVariation > 4) {
            throw new IllegalArgumentException("custom_variation must be between 1 and 4 for shaped blocks");
        }

        // Get the actual material to place
        this.placedMaterial = blockType.getMaterial(customVariation);

        // Parse model
        this.model = section.getString("model");

        // Parse breaking
        this.breaking = new BlockBreaking(section, getItemID());
        this.placeable = section.contains("placeable") ? new Placeable(section) : null;

        // Parse light
        this.light = new LightMechanic(section);

        // Parse limited placing
        ConfigurationSection limitedPlacingSection = section.getConfigurationSection("limited_placing");
        this.limitedPlacing = limitedPlacingSection != null ? new LimitedPlacing(limitedPlacingSection) : null;

        // Parse block sounds
        ConfigurationSection blockSoundsSection = section.getConfigurationSection("block_sounds");
        this.blockSounds = blockSoundsSection != null ? new BlockSounds(blockSoundsSection) : null;

        this.blockEvents = new BlockEvents(section, getItemID());
    }

    public ShapedBlockType getBlockType() {
        return blockType;
    }

    public boolean canPlaceOn(org.bukkit.block.BlockFace face) { return placeable == null || placeable.canPlaceOn(face); }
    public boolean canPlaceOn(org.bukkit.block.BlockFace face, Block block) { return placeable == null || placeable.canPlaceOn(face, block); }

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
        return getDrop(new ItemStack(Material.AIR));
    }

    public Drop getDrop(ItemStack tool) {
        return breaking.drop(tool);
    }

    public BlockBreaking.DurabilityAction getDurabilityAction(ItemStack tool) {
        return breaking.durabilityAction(tool);
    }

    public double getHardness() {
        return getHardness(new ItemStack(Material.AIR));
    }

    public double getHardness(ItemStack tool) {
        return breaking.hardness(tool);
    }

    public boolean hasHardness() {
        return hasHardness(new ItemStack(Material.AIR));
    }

    public boolean hasHardness(ItemStack tool) {
        return breaking.hasHardness(tool);
    }

    public double getAttributeSpeedMultiplier(ItemStack tool, Material blockType) {
        return breaking.attributeSpeedMultiplier(tool, blockType);
    }

    public double getPacketSpeedMultiplier(ItemStack tool, Material blockType) {
        return breaking.packetSpeedMultiplier(tool, blockType);
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

    public boolean hasBlockEvents() {
        return !blockEvents.isEmpty();
    }

    public boolean runBlockEvents(Player player, Action action) {
        return blockEvents.run(player, action);
    }

    public static String getItemId(CustomBlockData blockData) {
        String itemId = blockData.get(SHAPED_BLOCK_KEY, PersistentDataType.STRING);
        if (itemId != null) return itemId;

        return blockData.get(LEGACY_SHAPED_BLOCK_KEY, PersistentDataType.STRING);
    }

    public static void setItemId(CustomBlockData blockData, String itemId) {
        blockData.set(SHAPED_BLOCK_KEY, PersistentDataType.STRING, itemId);
        blockData.remove(LEGACY_SHAPED_BLOCK_KEY);
    }

    public static void removeItemId(CustomBlockData blockData) {
        blockData.remove(SHAPED_BLOCK_KEY);
        blockData.remove(LEGACY_SHAPED_BLOCK_KEY);
    }
}
