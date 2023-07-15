package io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.limitedplacing.LimitedPlacing;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.sapling.SaplingMechanic;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.oraxen.utils.drops.Loot;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class StringBlockMechanic extends Mechanic {

    private final int customVariation;
    private final Drop drop;
    private final BlockSounds blockSounds;
    private final LimitedPlacing limitedPlacing;
    private String model;
    private final int hardness;
    private final int light;

    private final List<String> randomPlaceBlock;
    private final SaplingMechanic saplingMechanic;
    private final boolean isTall;

    @SuppressWarnings("unchecked")
    public StringBlockMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        /*
         * We give: - an instance of the Factory which created the mechanic - the
         * section used to configure the mechanic
         */
        super(mechanicFactory, section);
        if (section.isString("model"))
            model = section.getString("model");

        customVariation = section.getInt("custom_variation");
        isTall = section.getBoolean("is_tall", false);

        List<Loot> loots = new ArrayList<>();
        if (section.isConfigurationSection("drop")) {
            ConfigurationSection drop = section.getConfigurationSection("drop");
            for (LinkedHashMap<String, Object> lootConfig : (List<LinkedHashMap<String, Object>>)
                    drop.getList("loots"))
                loots.add(new Loot(lootConfig));

            if (drop.isString("minimal_type")) {
                StringBlockMechanicFactory mechanic = (StringBlockMechanicFactory) mechanicFactory;
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
            drop = new Drop(loots, false, false, getItemID());

        hardness = section.getInt("hardness", 1);

        light = section.getInt("light", -1);

        ConfigurationSection randomPlaceSection = section.getConfigurationSection("random_place");
        if (randomPlaceSection != null) {
            randomPlaceBlock = randomPlaceSection.getStringList("block");
        } else randomPlaceBlock = new ArrayList<>();

        ConfigurationSection saplingSection = section.getConfigurationSection("sapling");
        if (saplingSection != null) {
            saplingMechanic = new SaplingMechanic(getItemID(), saplingSection);
            ((StringBlockMechanicFactory) getFactory()).registerSaplingMechanic();
        } else saplingMechanic = null;

        ConfigurationSection limitedSection = section.getConfigurationSection("limited_placing");
        if (limitedSection != null) {
            limitedPlacing = new LimitedPlacing(limitedSection);
        } else limitedPlacing = null;

        ConfigurationSection blockSoundsSection = section.getConfigurationSection("block_sounds");
        if (blockSoundsSection != null) {
            blockSounds = new BlockSounds(blockSoundsSection);
        } else blockSounds = null;
    }

    public String getModel(ConfigurationSection section) {
        if (model != null)
            return model;
        // use the itemstack model if block model isn't set
        return section.getString("Pack.model");
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
        return light <= -1;
    }

    public int getLight() {
        return light;
    }

    public boolean hasRandomPlace() {
        return !randomPlaceBlock.isEmpty();
    }

    public List<String> getRandomPlaceBlock() {
        return randomPlaceBlock;
    }

}
