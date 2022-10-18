package io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock;

import io.th0rgal.oraxen.compatibilities.CompatibilitiesManager;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.sapling.SaplingMechanic;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.oraxen.utils.drops.Loot;
import io.th0rgal.oraxen.utils.limitedplacing.LimitedPlacing;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

public class StringBlockMechanic extends Mechanic {

    protected final boolean hasHardness;
    private final int customVariation;
    private final Drop drop;
    private final BlockSounds blockSounds;
    private final LimitedPlacing limitedPlacing;
    private String model;
    private int period;
    private final int light;

    private final boolean hasRandomPlace;
    private List<String> randomPlaceBlock;
    private final SaplingMechanic saplingMechanic;

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

        // hardness requires protocollib
        if (CompatibilitiesManager.hasPlugin("ProtocolLib") && section.isInt("hardness")) {
            hasHardness = true;
            period = section.getInt("hardness");
        } else hasHardness = false;

        light = section.getInt("light", -1);

        if (section.isConfigurationSection("random_place")) {
            ConfigurationSection randomPlace = section.getConfigurationSection("random_place");
            hasRandomPlace = true;
            randomPlaceBlock = randomPlace.getStringList("block");
        } else hasRandomPlace = false;

        if (section.isConfigurationSection("sapling")) {
            saplingMechanic = new SaplingMechanic(getItemID(), Objects.requireNonNull(section.getConfigurationSection("sapling")));
            ((StringBlockMechanicFactory) getFactory()).registerSaplingMechanic();
        } else saplingMechanic = null;

        if (section.isConfigurationSection("limited_placing")) {
            limitedPlacing = new LimitedPlacing(Objects.requireNonNull(section.getConfigurationSection("limited_placing")));
        } else limitedPlacing = null;

        if (section.isConfigurationSection("block_sounds")) {
            blockSounds = new BlockSounds(Objects.requireNonNull(section.getConfigurationSection("block_sounds")));
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

    public int getCustomVariation() {
        return customVariation;
    }

    public Drop getDrop() {
        return drop;
    }

    public int getPeriod() {
        return period;
    }

    public int getLight() {
        return light;
    }

    public boolean hasRandomPlace() {
        return hasRandomPlace;
    }

    public List<String> getRandomPlaceBlock() {
        return randomPlaceBlock;
    }

}
