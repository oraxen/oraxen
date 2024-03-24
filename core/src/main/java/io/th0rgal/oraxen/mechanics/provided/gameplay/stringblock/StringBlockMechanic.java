package io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock;

import io.th0rgal.oraxen.compatibilities.provided.blocklocker.BlockLockerMechanic;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.light.LightMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.limitedplacing.LimitedPlacing;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.sapling.SaplingMechanic;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import io.th0rgal.oraxen.utils.drops.Drop;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Tripwire;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class StringBlockMechanic extends Mechanic {

    private final int customVariation;
    private final Drop drop;
    private final BlockSounds blockSounds;
    private final LimitedPlacing limitedPlacing;
    private final Key model;
    private final int hardness;
    private final LightMechanic light;

    private final List<String> randomPlaceBlock;
    private final SaplingMechanic saplingMechanic;
    private final boolean isTall;

    private final BlockLockerMechanic blockLocker;
    private final Tripwire blockData;

    @SuppressWarnings("unchecked")
    public StringBlockMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        /*
         * We give: - an instance of the Factory which created the mechanic - the
         * section used to configure the mechanic
         */
        super(mechanicFactory, section);

        model = Key.key(section.getString("model", section.getParent().getString("Pack.model", getItemID())));
        customVariation = section.getInt("custom_variation");
        blockData = createBlockData();
        isTall = section.getBoolean("is_tall", false);
        hardness = section.getInt("hardness", 1);
        light = new LightMechanic(section);

        ConfigurationSection dropSection = section.getConfigurationSection("drop");
        drop = dropSection != null ? Drop.createDrop(StringBlockMechanicFactory.getInstance().toolTypes, dropSection, getItemID()) : new Drop(new ArrayList<>(), false, false, getItemID());

        ConfigurationSection randomPlaceSection = section.getConfigurationSection("random_place");
        randomPlaceBlock = randomPlaceSection != null ? randomPlaceSection.getStringList("blocks") : new ArrayList<>();

        ConfigurationSection saplingSection = section.getConfigurationSection("sapling");
        saplingMechanic = saplingSection != null ? new SaplingMechanic(getItemID(), saplingSection) : null;

        ConfigurationSection limitedSection = section.getConfigurationSection("limited_placing");
        limitedPlacing = limitedSection != null ? new LimitedPlacing(limitedSection) : null;

        ConfigurationSection blockSoundsSection = section.getConfigurationSection("block_sounds");
        blockSounds = blockSoundsSection != null ? new BlockSounds(blockSoundsSection) : null;

        ConfigurationSection blockLockerSection = section.getConfigurationSection("blocklocker");
        blockLocker = blockLockerSection != null ? new BlockLockerMechanic(blockLockerSection) : null;
    }

    public Tripwire blockData() {
        return blockData;
    }

    private Tripwire createBlockData() {
        Tripwire tripwire = ((Tripwire) Bukkit.createBlockData(Material.TRIPWIRE));
        if (Settings.LEGACY_NOTEBLOCKS.toBool()) {
            int i = 0;
            for (BlockFace face : new BlockFace[]{BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH, BlockFace.NORTH})
                tripwire.setFace(face, (customVariation & 0x1 << i++) != 0);
            tripwire.setAttached((customVariation & 0x1 << i++) != 0);
            tripwire.setDisarmed((customVariation & 0x1 << i++) != 0);
            tripwire.setPowered((customVariation & 0x1 << i) != 0);
        } else {
            tripwire.setFace(BlockFace.NORTH, (customVariation & 0x1) != 0);
            tripwire.setFace(BlockFace.SOUTH, (customVariation & 0x2) != 0);
            tripwire.setFace(BlockFace.EAST, (customVariation & 0x4) != 0);
            tripwire.setFace(BlockFace.WEST, (customVariation & 0x8) != 0);
            tripwire.setAttached((customVariation & 0x10) != 0);
            tripwire.setDisarmed((customVariation & 0x20) != 0);
            tripwire.setPowered((customVariation & 0x40) != 0);
        }

        return tripwire;
    }

    public Key getModel() {
        return model;
    }

    public boolean hasBlockSounds() {
        return blockSounds != null;
    }
    public BlockSounds getBlockSounds() {
        return blockSounds;
    }

    public boolean hasLimitedPlacing() { return limitedPlacing != null; }
    public LimitedPlacing getLimitedPlacing() { return limitedPlacing; }

    public boolean isSapling() { return saplingMechanic != null; }
    public SaplingMechanic getSaplingMechanic() { return saplingMechanic; }

    public boolean isTall() { return isTall; }

    public int getCustomVariation() {
        return customVariation;
    }

    public Drop getDrop() {
        return drop;
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

    public boolean hasRandomPlace() {
        return !randomPlaceBlock.isEmpty();
    }

    public List<String> getRandomPlaceBlock() {
        return randomPlaceBlock;
    }

    public BlockLockerMechanic getBlockLocker() {
        return blockLocker;
    }

}
