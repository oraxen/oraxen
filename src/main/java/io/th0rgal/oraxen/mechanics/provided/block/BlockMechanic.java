package io.th0rgal.oraxen.mechanics.provided.block;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.oraxen.utils.drops.Loot;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class BlockMechanic extends Mechanic {

    private String model;
    private final int customVariation;
    private final Drop drop;
    private final Sound breakSound;

    @SuppressWarnings("unchecked")
    public BlockMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        /*
         * We give: - an instance of the Factory which created the mechanic - the
         * section used to configure the mechanic
         */
        super(mechanicFactory, section);
        if (section.isString("model"))
            this.model = section.getString("model");

        this.customVariation = section.getInt("custom_variation");

        if (section.isString("break_sound"))
            this.breakSound = Sound.valueOf(section.getString("break_sound").toUpperCase());
        else
            this.breakSound = null;

        List<Loot> loots = new ArrayList<>();
        ConfigurationSection drop = section.getConfigurationSection("drop");
        for (LinkedHashMap<String, Object> lootConfig : (List<LinkedHashMap<String, Object>>) drop.getList("loots")) {
            loots.add(new Loot(lootConfig));
        }

        if (drop.isString("minimal_type")) {
            BlockMechanicFactory mechanic = (BlockMechanicFactory) mechanicFactory;
            this.drop = new Drop(mechanic.toolTypes, loots, drop.getBoolean("silktouch"), drop.getBoolean("fortune"), getItemID(),
                drop.getString("minimal_type"));
        } else
            this.drop = new Drop(loots, drop.getBoolean("silktouch"), drop.getBoolean("fortune"), getItemID());
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
        return this.breakSound != null;
    }

    public Sound getBreakSound() {
        return this.breakSound;
    }

}
