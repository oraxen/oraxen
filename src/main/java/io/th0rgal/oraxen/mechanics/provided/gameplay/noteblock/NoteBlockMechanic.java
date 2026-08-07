package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.compatibilities.provided.blocklocker.BlockLockerMechanic;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockBreaking;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockEvents;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.Placeable;
import io.th0rgal.oraxen.mechanics.provided.gameplay.light.LightMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.limitedplacing.LimitedPlacing;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.directional.DirectionalBlock;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.farmblock.FarmBlockDryout;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.logstrip.LogStripping;
import io.th0rgal.oraxen.mechanics.provided.gameplay.storage.StorageMechanic;
import io.th0rgal.oraxen.utils.actions.ClickAction;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import io.th0rgal.oraxen.utils.drops.Drop;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class NoteBlockMechanic extends Mechanic {

    public static final NamespacedKey FARMBLOCK_KEY = new NamespacedKey(OraxenPlugin.get(), "farmblock");
    private final int customVariation;
    private final BlockBreaking breaking;
    private final Placeable placeable;
    private final LimitedPlacing limitedPlacing;
    private final StorageMechanic storage;
    private final BlockSounds blockSounds;
    private String model;
    private final LightMechanic light;
    private final boolean canIgnite;
    private final boolean isFalling;
    private final boolean blastResistant;
    private final boolean immovable;
    private final FarmBlockDryout farmBlockDryout;
    private final LogStripping logStripping;
    private final DirectionalBlock directionalBlock;
    private final List<ClickAction> clickActions;
    private final BlockEvents blockEvents;
    private final BlockLockerMechanic blockLocker;

    @SuppressWarnings("unchecked")
    public NoteBlockMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        /*
         * We give: - an instance of the Factory which created the mechanic - the
         * section used to configure the mechanic
         */
        super(mechanicFactory, section);

        model = section.getString("model");
        customVariation = section.getInt("custom_variation");
        breaking = new BlockBreaking(section, getItemID());
        placeable = section.contains("placeable") ? new Placeable(section) : null;

        light = new LightMechanic(section);
        clickActions = ClickAction.parseList(section);
        blockEvents = new BlockEvents(section, getItemID());
        canIgnite = section.getBoolean("can_ignite", false);
        isFalling = section.getBoolean("is_falling", false);
        blastResistant = section.getBoolean("blast_resistant", false);
        immovable = section.getBoolean("immovable", false);

        ConfigurationSection farmBlockSection = section.getConfigurationSection("farmblock");
        farmBlockDryout = farmBlockSection != null ? new FarmBlockDryout(getItemID(), farmBlockSection) : null;
        if (farmBlockDryout != null) ((NoteBlockMechanicFactory) getFactory()).registerFarmBlock();

        ConfigurationSection logStripSection = section.getConfigurationSection("logStrip");
        logStripping = logStripSection != null ? new LogStripping(logStripSection) : null;

        ConfigurationSection directionalSection = section.getConfigurationSection("directional");
        directionalBlock = directionalSection != null ? new DirectionalBlock(directionalSection) : null;

        ConfigurationSection limitedPlacingSection = section.getConfigurationSection("limited_placing");
        limitedPlacing = limitedPlacingSection != null ? new LimitedPlacing(limitedPlacingSection) : null;

        ConfigurationSection storageSection = section.getConfigurationSection("storage");
        storage = storageSection != null ? new StorageMechanic(storageSection) : null;

        ConfigurationSection blockSoundsSection = section.getConfigurationSection("block_sounds");
        blockSounds = blockSoundsSection != null ? new BlockSounds(blockSoundsSection) : null;

        ConfigurationSection blockLockerSection = section.getConfigurationSection("blocklocker");
        blockLocker = blockLockerSection != null ? new BlockLockerMechanic(blockLockerSection) : null;
    }

    public boolean canPlaceOn(org.bukkit.block.BlockFace face) { return placeable == null || placeable.canPlaceOn(face); }
    public boolean canPlaceOn(org.bukkit.block.BlockFace face, Block block) { return placeable == null || placeable.canPlaceOn(face, block); }

    public boolean hasLimitedPlacing() { return limitedPlacing != null; }
    public LimitedPlacing getLimitedPlacing() { return limitedPlacing; }

    public boolean isStorage() { return storage != null; }
    public StorageMechanic getStorage() { return storage; }

    public boolean hasBlockSounds() { return blockSounds != null; }
    public BlockSounds getBlockSounds() { return blockSounds; }

    public boolean hasDryout() { return farmBlockDryout != null; }
    public FarmBlockDryout getDryout() { return farmBlockDryout; }

    public boolean isLog() {
        if (isDirectional() && !getDirectional().isParentBlock()) {
            return logStripping != null || directionalBlock.getParentMechanic().isLog();
        } else return logStripping != null;
    }
    public LogStripping getLog() { return logStripping; }

    public boolean isFalling() {
        if (isDirectional() && !directionalBlock.isParentBlock()) {
            return isFalling || directionalBlock.getParentMechanic().isFalling();
        } else return isFalling;
    }

    public boolean isDirectional() { return directionalBlock != null; }
    public DirectionalBlock getDirectional() { return directionalBlock; }

    public BlockLockerMechanic getBlockLocker() {
        return blockLocker;
    }

    public String getModel(ConfigurationSection section) {
        if (model != null)
            return model;
        // use the itemstack model if block model isn't set
        return section.getString("Pack.model");
    }

    public int getCustomVariation() {
        return customVariation;
    }

    public Drop getDrop() {
        return getDrop(new ItemStack(Material.AIR));
    }

    public Drop getDrop(ItemStack tool) {
        if (isDirectional() && !getDirectional().isParentBlock() && !breaking.hasHardness(tool))
            return directionalBlock.getParentMechanic().getDrop(tool);
        return breaking.drop(tool);
    }

    public BlockBreaking.DurabilityAction getDurabilityAction(ItemStack tool) {
        BlockBreaking.DurabilityAction action = breaking.durabilityAction(tool);
        if (action != null) return action;
        if (isDirectional() && !getDirectional().isParentBlock())
            return directionalBlock.getParentMechanic().getDurabilityAction(tool);
        return null;
    }

    public boolean hasHardness() {
        return hasHardness(new ItemStack(Material.AIR));
    }

    public boolean hasHardness(ItemStack tool) {
        if (isDirectional() && !getDirectional().isParentBlock()) {
            return breaking.hasHardness(tool) || directionalBlock.getParentMechanic().hasHardness(tool);
        } else return breaking.hasHardness(tool);
    }

    public double getHardness() {
        return getHardness(new ItemStack(Material.AIR));
    }

    public double getHardness(ItemStack tool) {
        if (isDirectional() && !getDirectional().isParentBlock() && !breaking.hasHardness(tool))
            return directionalBlock.getParentMechanic().getHardness(tool);
        return breaking.hardness(tool);
    }

    public double getAttributeSpeedMultiplier(ItemStack tool, Material blockType) {
        if (isDirectional() && !getDirectional().isParentBlock() && !breaking.hasHardness(tool))
            return directionalBlock.getParentMechanic().getAttributeSpeedMultiplier(tool, blockType);
        return breaking.attributeSpeedMultiplier(tool, blockType);
    }

    public double getPacketSpeedMultiplier(ItemStack tool, Material blockType) {
        if (isDirectional() && !getDirectional().isParentBlock() && !breaking.hasHardness(tool))
            return directionalBlock.getParentMechanic().getPacketSpeedMultiplier(tool, blockType);
        return breaking.packetSpeedMultiplier(tool, blockType);
    }

    public boolean hasLight() {
        if (isDirectional() && !getDirectional().isParentBlock()) {
            return light.hasLightLevel() || directionalBlock.getParentMechanic().getLight().hasLightLevel();
        } else return light.hasLightLevel();
    }

    public LightMechanic getLight() {
        return light;
    }

    public boolean canIgnite() {
        if (isDirectional() && !getDirectional().isParentBlock()) {
            return canIgnite || directionalBlock.getParentMechanic().canIgnite();
        } else return canIgnite;
    }

    public boolean hasClickActions() { return !clickActions.isEmpty(); }

    public boolean hasBlockEvents() { return !blockEvents.isEmpty(); }

    public boolean runBlockEvents(final Player player, final Action action) { return blockEvents.run(player, action); }

    public void runClickActions(final Player player) {
        for (final ClickAction action : clickActions) {
            if (action.canRun(player)) {
                action.performActions(player);
            }
        }
    }

    public boolean isInteractable() {
        return hasClickActions() || hasBlockEvents() || isStorage();
    }

    public boolean isBlastResistant() {
        if (isDirectional() && !getDirectional().isParentBlock()) {
            return blastResistant || directionalBlock.getParentMechanic().isBlastResistant();
        } else return blastResistant;
    }

    public boolean isImmovable() {
        if (isDirectional() && !getDirectional().isParentBlock()) {
            return immovable || directionalBlock.getParentMechanic().isImmovable();
        } else return immovable;
    }

}
