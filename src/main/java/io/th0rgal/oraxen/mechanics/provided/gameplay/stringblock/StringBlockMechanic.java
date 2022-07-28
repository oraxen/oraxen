package io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock;

import io.th0rgal.oraxen.compatibilities.CompatibilitiesManager;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.sapling.SaplingMechanic;
import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.oraxen.utils.drops.Loot;
import io.th0rgal.oraxen.utils.limitedplacing.LimitedPlacing;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class StringBlockMechanic extends Mechanic {

    protected final boolean hasHardness;
    private final int customVariation;
    private final Drop drop;
    private final LimitedPlacing limitedPlacing;
    private final String breakSound;
    private final String placeSound;
    private final String stepSound;
    private final String hitSound;
    private final String fallSound;
    private String model;
    private int period;
    private final int light;
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
        placeSound = section.getString("place_sound", null);
        breakSound = section.getString("break_sound", null);
        stepSound = section.getString("step_sound", null);
        hitSound = section.getString("hit_sound", null);
        fallSound = section.getString("fall_sound", null);

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

        if (section.isConfigurationSection("sapling")) {
            saplingMechanic = new SaplingMechanic(getItemID(), section.getConfigurationSection("sapling"));
            ((StringBlockMechanicFactory) getFactory()).registerSaplingMechanic();
        } else saplingMechanic = null;

        if (section.isConfigurationSection("limited_placing")) {
            limitedPlacing = new LimitedPlacing(section.getConfigurationSection("limited_placing"));
        } else limitedPlacing = null;
    }

    public String getModel(ConfigurationSection section) {
        if (model != null)
            return model;
        // use the itemstack model if block model isn't set
        return section.getString("Pack.model");
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

    public boolean hasBreakSound() {
        return breakSound != null;
    }
    public String getBreakSound() {
        return validateReplacedSounds(breakSound);
    }

    public boolean hasPlaceSound() {
        return placeSound != null;
    }
    public String getPlaceSound() {
        return validateReplacedSounds(placeSound);
    }

    public boolean hasStepSound() { return stepSound != null; }
    public String getStepSound() { return validateReplacedSounds(stepSound); }

    public boolean hasHitSound() { return hitSound != null; }
    public String getHitSound() { return validateReplacedSounds(hitSound); }

    public boolean hasFallSound() { return fallSound != null; }
    public String getFallSound() { return validateReplacedSounds(fallSound); }
    private String validateReplacedSounds(String sound) {
        if (sound.startsWith("block.wood"))
            return sound.replace("block.wood", "required.wood.");
        else if (sound.startsWith("block.stone"))
            return sound.replace("block.stone", "required.stone.");
        else return sound;
    }

    public int getPeriod() {
        return period;
    }

    public int getLight() {
        return light;
    }

}
