package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block;

import io.th0rgal.oraxen.compatibilities.provided.blocklocker.BlockLockerMechanic;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.BreakableMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.light.LightMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.limitedplacing.LimitedPlacing;
import io.th0rgal.oraxen.utils.actions.ClickAction;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class CustomBlockMechanic extends Mechanic {

    private final CustomBlockType type;
    private final int customVariation;
    private final Key model;
    private final BlockData blockData;

    private final BlockSounds blockSounds;
    private final LightMechanic light;
    private final LimitedPlacing limitedPlacing;
    private final List<ClickAction> clickActions;
    private final BlockLockerMechanic blockLocker;
    private final BreakableMechanic breakable;
    private final boolean blastResistant;

    public CustomBlockMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);

        type = CustomBlockType.fromMechanicSection(section);
        model = Key.key(section.getString("model", section.getParent().getString("Pack.model", getItemID())));
        customVariation = section.getInt("custom_variation");
        blockData = createBlockData();

        clickActions = ClickAction.parseList(section);
        light = new LightMechanic(section);
        breakable = new BreakableMechanic(section);
        blastResistant = section.getBoolean("blast_resistant");

        ConfigurationSection limitedPlacingSection = section.getConfigurationSection("limited_placing");
        limitedPlacing = limitedPlacingSection != null ? new LimitedPlacing(limitedPlacingSection) : null;

        ConfigurationSection blockSoundsSection = section.getConfigurationSection("block_sounds");
        blockSounds = blockSoundsSection != null ? new BlockSounds(blockSoundsSection) : null;

        ConfigurationSection blockLockerSection = section.getConfigurationSection("blocklocker");
        blockLocker = blockLockerSection != null ? new BlockLockerMechanic(blockLockerSection) : null;
    }

    public CustomBlockType type() {
        return type;
    }

    public BlockData createBlockData() {
        return Material.AIR.createBlockData();
    }

    public BlockData blockData() {
        return this.blockData;
    }

    public Key model() {
        return model;
    }

    public int customVariation() {
        return customVariation;
    }

    public BreakableMechanic breakable() {
        return breakable;
    }

    public boolean hasLight() {
        return light != null && light.lightBlocks().isEmpty();
    }

    public LightMechanic light() {
        return light;
    }

    public boolean hasLimitedPlacing() {
        return limitedPlacing != null;
    }

    public LimitedPlacing limitedPlacing() {
        return limitedPlacing;
    }

    public boolean hasBlockSounds() {
        return blockSounds != null;
    }

    public BlockSounds blockSounds() {
        return blockSounds;
    }

    public boolean hasClickActions() {
        return !clickActions.isEmpty();
    }

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

    public boolean isBlastResistant() {
        return blastResistant;
    }
}
