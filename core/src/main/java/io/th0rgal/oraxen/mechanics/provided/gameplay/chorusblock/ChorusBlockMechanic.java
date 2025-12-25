package io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock;

import io.th0rgal.oraxen.compatibilities.provided.blocklocker.BlockLockerMechanic;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.light.LightMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.limitedplacing.LimitedPlacing;
import io.th0rgal.oraxen.utils.actions.ClickAction;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import io.th0rgal.oraxen.utils.drops.Drop;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class ChorusBlockMechanic extends Mechanic {

    private final int customVariation;
    private String model;
    private final Drop drop;
    private final BlockSounds blockSounds;
    private final LimitedPlacing limitedPlacing;
    private final int hardness;
    private final LightMechanic light;
    private final boolean isFalling;
    private final boolean blastResistant;
    private final boolean immovable;
    private final BlockLockerMechanic blockLocker;
    private final List<ClickAction> clickActions;

    // Cached blockData for efficient lookup
    private final MultipleFacing blockData;

    @SuppressWarnings("unchecked")
    public ChorusBlockMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);

        model = section.getString("model");
        customVariation = section.getInt("custom_variation");
        hardness = section.getInt("hardness", 1);
        light = new LightMechanic(section);

        isFalling = section.getBoolean("is_falling", false);
        blastResistant = section.getBoolean("blast_resistant", false);
        immovable = section.getBoolean("immovable", false);

        ConfigurationSection dropSection = section.getConfigurationSection("drop");
        drop = dropSection != null
                ? Drop.createDrop(ChorusBlockMechanicFactory.getInstance().toolTypes, dropSection, getItemID())
                : new Drop(new ArrayList<>(), false, false, getItemID());

        ConfigurationSection limitedSection = section.getConfigurationSection("limited_placing");
        limitedPlacing = limitedSection != null ? new LimitedPlacing(limitedSection) : null;

        ConfigurationSection blockSoundsSection = section.getConfigurationSection("block_sounds");
        blockSounds = blockSoundsSection != null ? new BlockSounds(blockSoundsSection) : null;

        ConfigurationSection blockLockerSection = section.getConfigurationSection("blocklocker");
        blockLocker = blockLockerSection != null ? new BlockLockerMechanic(blockLockerSection) : null;

        clickActions = ClickAction.parseList(section);

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
        return drop;
    }

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
        return hardness != -1;
    }

    public int getHardness() {
        return hardness;
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

    public BlockLockerMechanic getBlockLocker() {
        return blockLocker;
    }

    public List<ClickAction> getClickActions() {
        return clickActions;
    }

    public boolean hasClickActions() {
        return !clickActions.isEmpty();
    }
}
