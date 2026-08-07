package io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockBreaking;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockEvents;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.Placeable;
import io.th0rgal.oraxen.mechanics.provided.gameplay.light.LightMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.limitedplacing.LimitedPlacing;
import io.th0rgal.oraxen.mechanics.provided.gameplay.storage.StorageMechanic;
import io.th0rgal.oraxen.utils.actions.ClickAction;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import io.th0rgal.oraxen.utils.drops.Drop;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ChorusBlockMechanic extends Mechanic {

    private final int customVariation;
    private String model;
    private final BlockBreaking breaking;
    private final Placeable placeable;
    private final BlockSounds blockSounds;
    private final LimitedPlacing limitedPlacing;
    private final StorageMechanic storage;
    private final LightMechanic light;
    private final boolean isFalling;
    private final boolean blastResistant;
    private final boolean immovable;
    private final List<ClickAction> clickActions;
    private final BlockEvents blockEvents;
    private final float seatHeight;
    private final boolean hasSeat;
    private final boolean hasSeatYaw;
    private final float seatYaw;

    // Cached blockData for efficient lookup
    private final MultipleFacing blockData;

    @SuppressWarnings("unchecked")
    public ChorusBlockMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);

        model = section.getString("model");
        customVariation = section.getInt("custom_variation");
        breaking = new BlockBreaking(section, getItemID());
        placeable = section.contains("placeable") ? new Placeable(section) : null;
        light = new LightMechanic(section);

        isFalling = section.getBoolean("is_falling", false);
        blastResistant = section.getBoolean("blast_resistant", false);
        immovable = section.getBoolean("immovable", false);

        ConfigurationSection limitedSection = section.getConfigurationSection("limited_placing");
        limitedPlacing = limitedSection != null ? new LimitedPlacing(limitedSection) : null;

        ConfigurationSection blockSoundsSection = section.getConfigurationSection("block_sounds");
        blockSounds = blockSoundsSection != null ? new BlockSounds(blockSoundsSection) : null;

        ConfigurationSection storageSection = section.getConfigurationSection("storage");
        storage = storageSection != null ? new StorageMechanic(storageSection) : null;

        clickActions = ClickAction.parseList(section);
        blockEvents = new BlockEvents(section, getItemID());

        // Parse seat configuration
        ConfigurationSection seatSection = section.getConfigurationSection("seat");
        if (seatSection != null) {
            hasSeat = true;
            seatHeight = (float) seatSection.getDouble("height", 0.5);
            hasSeatYaw = seatSection.contains("yaw");
            seatYaw = hasSeatYaw ? (float) seatSection.getDouble("yaw") : 0;
        } else {
            hasSeat = false;
            seatHeight = 0;
            hasSeatYaw = false;
            seatYaw = 0;
        }

        // Cache the blockData for this mechanic
        blockData = ChorusBlockMechanicFactory.createChorusData(customVariation);
    }

    public String getModel(ConfigurationSection section) {
        return model != null ? model : section.getString("Pack.model");
    }

    public int getCustomVariation() {
        return customVariation;
    }

    public MultipleFacing createBlockData() {
        return ChorusBlockMechanicFactory.createChorusData(customVariation);
    }

    public MultipleFacing getBlockData() {
        return blockData;
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

    public boolean canPlaceOn(org.bukkit.block.BlockFace face) { return placeable == null || placeable.canPlaceOn(face); }
    public boolean canPlaceOn(org.bukkit.block.BlockFace face, Block block) { return placeable == null || placeable.canPlaceOn(face, block); }

    public boolean hasBlockSounds() {
        return blockSounds != null;
    }

    public BlockSounds getBlockSounds() {
        return blockSounds;
    }

    public boolean hasLimitedPlacing() {
        return limitedPlacing != null;
    }

    public LimitedPlacing getLimitedPlacing() {
        return limitedPlacing;
    }

    public boolean hasHardness() {
        return hasHardness(new ItemStack(Material.AIR));
    }

    public boolean hasHardness(ItemStack tool) {
        return breaking.hasHardness(tool);
    }

    public double getHardness() {
        return getHardness(new ItemStack(Material.AIR));
    }

    public double getHardness(ItemStack tool) {
        return breaking.hardness(tool);
    }

    public double getAttributeSpeedMultiplier(ItemStack tool, Material blockType) {
        return breaking.attributeSpeedMultiplier(tool, blockType);
    }

    public double getPacketSpeedMultiplier(ItemStack tool, Material blockType) {
        return breaking.packetSpeedMultiplier(tool, blockType);
    }

    public boolean hasLight() {
        return light.hasLightLevel();
    }

    public LightMechanic getLight() {
        return light;
    }

    public boolean isFalling() {
        return isFalling;
    }

    public boolean isBlastResistant() {
        return blastResistant;
    }

    public boolean isImmovable() {
        return immovable;
    }

    public List<ClickAction> getClickActions() {
        return clickActions;
    }

    public boolean hasClickActions() {
        return !clickActions.isEmpty();
    }

    public boolean hasBlockEvents() {
        return !blockEvents.isEmpty();
    }

    public boolean runBlockEvents(final Player player, final Action action) {
        return blockEvents.run(player, action);
    }

    public void runClickActions(final Player player) {
        for (final ClickAction action : clickActions) {
            if (action.canRun(player)) {
                action.performActions(player);
            }
        }
    }

    public boolean isStorage() {
        return storage != null;
    }

    public StorageMechanic getStorage() {
        return storage;
    }

    public boolean hasSeat() {
        return hasSeat;
    }

    public float getSeatHeight() {
        return seatHeight;
    }

    public boolean hasSeatYaw() {
        return hasSeatYaw;
    }

    public float getSeatYaw() {
        return seatYaw;
    }

    public boolean isInteractable() {
        return hasClickActions() || hasBlockEvents() || isStorage() || hasSeat();
    }
}
