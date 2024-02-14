package io.th0rgal.oraxen.mechanics.provided.gameplay.block;

import com.google.gson.JsonObject;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.limitedplacing.LimitedPlacing;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.oraxen.utils.drops.Loot;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class BlockMechanic extends Mechanic {

    private String model;
    private final int customVariation;
    private final Drop drop;
    private final boolean canIgnite;
    private final LimitedPlacing limitedPlacing;
    private final BlockSounds blockSounds;

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


        List<Loot> loots = new ArrayList<>();
        ConfigurationSection drop = section.getConfigurationSection("drop");
        if (drop != null){
            for (LinkedHashMap<String, Object> lootConfig : (List<LinkedHashMap<String, Object>>) drop.getList("loots", new ArrayList<>()))
                loots.add(new Loot(lootConfig, getItemID()));
            if (drop.isString("minimal_type")) {
                BlockMechanicFactory mechanic = (BlockMechanicFactory) mechanicFactory;
                this.drop = new Drop(mechanic.toolTypes, loots, drop.getBoolean("silktouch"),
                        drop.getBoolean("fortune"),
                        getItemID(),
                        drop.getString("minimal_type"),
                        new ArrayList<>());
            } else
                this.drop = new Drop(loots, drop.getBoolean("silktouch"), drop.getBoolean("fortune"), getItemID());
        } else this.drop = null;

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

    public boolean hasLimitedPlacing() { return limitedPlacing != null; }
    public LimitedPlacing getLimitedPlacing() { return limitedPlacing; }

    public boolean hasBlockSounds() { return blockSounds != null; }
    public BlockSounds getBlockSounds() { return blockSounds; }

    public int getCustomVariation() {
        return customVariation;
    }

    public Drop getDrop() {
        return drop;
    }

    public boolean canIgnite() {
        return canIgnite;
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
