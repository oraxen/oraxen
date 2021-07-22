package io.th0rgal.oraxen.mechanics.provided.noteblock;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.oraxen.utils.drops.Loot;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class NoteBlockMechanic extends Mechanic {

    private String model;
    private final int customVariation;
    private final Drop drop;
    private final Sound breakSound;
    protected final boolean hasHardness;
    private int period;

    @SuppressWarnings("unchecked")
    public NoteBlockMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
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
        if (section.isConfigurationSection("drop")) {
            ConfigurationSection drop = section.getConfigurationSection("drop");
            for (LinkedHashMap<String, Object> lootConfig : (List<LinkedHashMap<String, Object>>)
                    drop.getList("loots"))
                loots.add(new Loot(lootConfig));

            if (drop.isString("minimal_type")) {
                NoteBlockMechanicFactory mechanic = (NoteBlockMechanicFactory) mechanicFactory;
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
            this.drop = new Drop(loots, false, false, getItemID());

        // hardness requires protocollib
        if (OraxenPlugin.getProtocolLib() && section.isInt("hardness")) {
            hasHardness = true;
            period = section.getInt("hardness");
        } else {
            hasHardness = false;
        }
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

    public int getPeriod() {
        return period;
    }

}
