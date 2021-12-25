package io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock;

import io.th0rgal.oraxen.compatibilities.CompatibilitiesManager;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.oraxen.utils.drops.Loot;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.type.Tripwire;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public class StringBlockMechanic extends Mechanic {

    protected final boolean hasHardness;
    private final int customVariation;
    private final Drop drop;
    private final String breakSound;
    private final String placeSound;
    private String model;
    private int period;
    private final int light;

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

        if (section.isString("break_sound"))
            breakSound = section.getString("break_sound");
        else
            breakSound = null;

        if (section.isString("place_sound"))
            placeSound = section.getString("place_sound");
        else
            placeSound = null;

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
        return drop;
    }

    public boolean hasBreakSound() {
        return breakSound != null;
    }

    public String getBreakSound() {
        return breakSound;
    }

    public boolean hasPlaceSound() {
        return placeSound != null;
    }

    public String getPlaceSound() {
        return placeSound;
    }

    public int getPeriod() {
        return period;
    }

    public int getLight() {
        return light;
    }

}
