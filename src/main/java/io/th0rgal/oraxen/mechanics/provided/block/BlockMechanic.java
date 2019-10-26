package io.th0rgal.oraxen.mechanics.provided.block;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.Loot;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class BlockMechanic extends Mechanic {

    private List<Loot> loots;
    private boolean defaultBreakAnimation;
    private String model;
    private int customVariation;

    @SuppressWarnings("unchecked")
    public BlockMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        /* We give:
        - an instance of the Factory which created the mechanic
        - the section used to configure the mechanic
         */
        super(mechanicFactory, section);
        loots = new ArrayList<>();
        for (LinkedHashMap<String, Object> lootConfig
                : (List<LinkedHashMap<String, Object>>) section.getList("loots")) {
            loots.add(new Loot(lootConfig));
        }

        if (!section.isConfigurationSection("break_animation")) {
            defaultBreakAnimation = true;
        } else {
            ConfigurationSection breakAnimation = section.getConfigurationSection("break_animation");
            defaultBreakAnimation = !breakAnimation.isBoolean("default") || breakAnimation.getBoolean("default");
        }

        // todo: use the itemstack model if block model isn't set
        this.model = section.getString("model");
        this.customVariation = section.getInt("custom_variation");
    }

    public String getModel() {
        return model;
    }

    public int getCustomVariation() {
        return customVariation;
    }

    public List<Loot> getLoots() {
        return loots;
    }

    public boolean isDefaultBreakAnimation() {
        return defaultBreakAnimation;
    }
}