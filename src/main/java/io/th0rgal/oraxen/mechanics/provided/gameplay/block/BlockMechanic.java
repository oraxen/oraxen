package io.th0rgal.oraxen.mechanics.provided.gameplay.block;

import com.google.gson.JsonObject;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.oraxen.utils.drops.Loot;
import org.bukkit.Sound;
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
    private final Sound breakSound;
    private final Sound placeSound;

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

        if (section.isString("break_sound"))
            breakSound = Sound.valueOf(section.getString("break_sound").toUpperCase());
        else
            breakSound = null;

        if (section.isString("place_sound"))
            placeSound = Sound.valueOf(section.getString("place_sound").toUpperCase());
        else
            placeSound = null;


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

    public Sound getBreakSound() {
        return breakSound;
    }

    public boolean hasPlaceSound() {
        return placeSound != null;
    }

    public Sound getPlaceSound() {
        return placeSound;
    }

    public static int getCode(final MultipleFacing blockData) {
        final List<BlockFace> properties = Arrays
                .asList(BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH, BlockFace.NORTH, BlockFace.DOWN, BlockFace.UP);
        int sum = 0;
        for (final BlockFace blockFace : blockData.getFaces())
            sum += (int) Math.pow(2, properties.indexOf(blockFace));
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
