package io.th0rgal.oraxen.mechanics.provided.block;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.oraxen.utils.drops.Loot;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class BlockMechanic extends Mechanic {

    private boolean defaultBreakAnimation;
    private String model;
    private int customVariation;
    private Drop drop;

    @SuppressWarnings("unchecked")
    public BlockMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        /* We give:
        - an instance of the Factory which created the mechanic
        - the section used to configure the mechanic
         */
        super(mechanicFactory, section);

        if (!section.isConfigurationSection("break_animation")) {
            defaultBreakAnimation = true;
        } else {
            ConfigurationSection breakAnimation = section.getConfigurationSection("break_animation");
            defaultBreakAnimation = !breakAnimation.isBoolean("default") || breakAnimation.getBoolean("default");
        }

        // todo: use the itemstack model if block model isn't set
        this.model = section.getString("model");
        this.customVariation = section.getInt("custom_variation");

        List<Loot> loots = new ArrayList<>();
        ConfigurationSection drop = section.getConfigurationSection("drop");
        for (LinkedHashMap<String, Object> lootConfig
                : (List<LinkedHashMap<String, Object>>) drop.getList("loots")) {
            loots.add(new Loot(lootConfig));
        }
        if (drop.isString("minimal_tool"))
            this.drop = new Drop(loots, drop.getBoolean("silktouch"),
                    getItemID(),
                    Material.getMaterial(drop.getString("minimal_tool")));
        else
            this.drop = new Drop(loots, drop.getBoolean("silktouch"), getItemID());
    }

    public String getModel() {
        return model;
    }

    public int getCustomVariation() {
        return customVariation;
    }

    public Drop getDrop() {
        return drop;
    }

    public boolean isDefaultBreakAnimation() {
        return defaultBreakAnimation;
    }
}