package io.th0rgal.oraxen.mechanics.provided.gameplay.block;

import com.google.gson.JsonObject;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.oraxen.utils.drops.Loot;
import io.th0rgal.oraxen.utils.limitedplacing.LimitedPlacing;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class BlockMechanic extends Mechanic {

    private String model;
    private final int customVariation;
    private final Drop drop;
    private final String breakSound;
    private final String placeSound;
    private final String stepSound;
    private final String hitSound;
    private final String fallSound;
    private final boolean canIgnite;
    private final LimitedPlacing limitedPlacing;

    @SuppressWarnings("unchecked")
    public BlockMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        /*
         * We give: - an instance of the Factory which created the mechanic - the
         * section used to configure the mechanic
         */
        super(mechanicFactory, section);
        if (section.isString("model"))
            model = section.getString("model");

        customVariation = section.getInt("custom_variation");
        canIgnite = section.getBoolean("can_ignite", false);

        placeSound = section.getString("place_sound", null);
        breakSound = section.getString("break_sound", null);
        stepSound = section.getString("step_sound", null);
        hitSound = section.getString("hit_sound", null);
        fallSound = section.getString("fall_sound", null);


        List<Loot> loots = new ArrayList<>();
        ConfigurationSection drop = section.getConfigurationSection("drop");
        for (LinkedHashMap<String, Object> lootConfig : (List<LinkedHashMap<String, Object>>) drop.getList("loots"))
            loots.add(new Loot(lootConfig));

        if (drop.isString("minimal_type")) {
            BlockMechanicFactory mechanic = (BlockMechanicFactory) mechanicFactory;
            this.drop = new Drop(mechanic.toolTypes, loots, drop.getBoolean("silktouch"),
                    drop.getBoolean("fortune"),
                    getItemID(),
                    drop.getString("minimal_type"),
                    new ArrayList<>());
        } else
            this.drop = new Drop(loots, drop.getBoolean("silktouch"), drop.getBoolean("fortune"), getItemID());

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

    public int getCustomVariation() {
        return customVariation;
    }

    public Drop getDrop() {
        return drop;
    }

    public boolean canIgnite() {
        return canIgnite;
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

    public static int getCode(final Block block) {
        int sum = 0;
        if (block.getType() == Material.MUSHROOM_STEM) {
            final MultipleFacing blockData = (MultipleFacing) block.getBlockData();
            final List<BlockFace> properties = Arrays
                    .asList(BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH, BlockFace.NORTH, BlockFace.DOWN, BlockFace.UP);
            for (final BlockFace blockFace : blockData.getFaces())
                sum += (int) Math.pow(2, properties.indexOf(blockFace));
        }

        return sum;
    }

    public static JsonObject getBlockstateWhenFields(final int code) {
        final JsonObject whenJson = new JsonObject();
        final String[] properties = new String[]{"up", "down", "north", "south", "west", "east"};
        for (int i = 0; i < properties.length; i++)
            whenJson.addProperty(properties[properties.length - 1 - i], (code & 0x1 << i) != 0);
        return whenJson;
    }

    public static void setBlockFacing(final MultipleFacing blockData, final int code) {
        final BlockFace[] properties = new BlockFace[]{BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH,
                BlockFace.NORTH, BlockFace.DOWN, BlockFace.UP};
        for (int i = 0; i < properties.length; i++) blockData.setFace(properties[i], (code & 0x1 << i) != 0);
    }


}
