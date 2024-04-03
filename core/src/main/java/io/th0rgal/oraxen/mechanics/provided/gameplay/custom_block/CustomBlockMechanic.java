package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block;

import io.th0rgal.oraxen.compatibilities.provided.blocklocker.BlockLockerMechanic;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.light.LightMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.limitedplacing.LimitedPlacing;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.noteblock.NoteBlockMechanicFactory;
import io.th0rgal.oraxen.utils.actions.ClickAction;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import io.th0rgal.oraxen.utils.drops.Drop;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public abstract class CustomBlockMechanic extends Mechanic {

    private final int customVariation;
    private final Key model;
    private final int hardness;
    private BlockData blockData;

    private final Drop drop;
    private final BlockSounds blockSounds;
    private final LightMechanic light;
    private final LimitedPlacing limitedPlacing;
    private final List<ClickAction> clickActions;
    private final BlockLockerMechanic blockLocker;

    public CustomBlockMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);

        model = Key.key(section.getString("model", section.getParent().getString("Pack.model", getItemID())));
        customVariation = section.getInt("custom_variation");
        hardness = section.getInt("hardness", 1);
        blockData = createBlockData();

        clickActions = ClickAction.parseList(section);
        light = new LightMechanic(section);

        ConfigurationSection dropSection = section.getConfigurationSection("drop");
        drop = dropSection != null ? Drop.createDrop(NoteBlockMechanicFactory.getInstance().toolTypes, dropSection, getItemID()) : new Drop(new ArrayList<>(), false, false, getItemID());

        ConfigurationSection limitedPlacingSection = section.getConfigurationSection("limited_placing");
        limitedPlacing = limitedPlacingSection != null ? new LimitedPlacing(limitedPlacingSection) : null;

        ConfigurationSection blockSoundsSection = section.getConfigurationSection("block_sounds");
        blockSounds = blockSoundsSection != null ? new BlockSounds(blockSoundsSection) : null;

        ConfigurationSection blockLockerSection = section.getConfigurationSection("blocklocker");
        blockLocker = blockLockerSection != null ? new BlockLockerMechanic(blockLockerSection) : null;
    }

    public BlockData createBlockData() {
        return Material.AIR.createBlockData();
    }

    public BlockData blockData() {
        return this.blockData;
    }

    public boolean hasLimitedPlacing() { return limitedPlacing != null; }
    public LimitedPlacing limitedPlacing() { return limitedPlacing; }

    public boolean hasBlockSounds() { return blockSounds != null; }
    public BlockSounds blockSounds() { return blockSounds; }

    public Key model() {
        return model;
    }

    public int customVariation() {
        return customVariation;
    }

    public Drop drop() {
        return drop;
    }

    public boolean hasHardness() {
        return hardness != -1;
    }

    public int hardness() {
        return hardness;
    }

    public boolean hasLight() {
        return light.hasLightLevel();
    }

    public LightMechanic light() {
        return light;
    }

    public boolean hasClickActions() { return !clickActions.isEmpty(); }

    public void runClickActions(final Player player) {
        for (final ClickAction action : clickActions) {
            if (action.canRun(player)) {
                action.performActions(player);
            }
        }
    }

    public BlockLockerMechanic blockLocker() {
        return blockLocker;
    }
}
