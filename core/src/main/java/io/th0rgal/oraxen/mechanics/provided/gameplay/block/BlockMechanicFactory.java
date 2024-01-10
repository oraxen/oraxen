package io.th0rgal.oraxen.mechanics.provided.gameplay.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockMechanicFactory extends MechanicFactory {

    private static final List<JsonObject> MUSHROOM_STEM_BLOCKSTATE_OVERRIDES = new ArrayList<>();
    private static final Map<Integer, BlockMechanic> BLOCK_PER_VARIATION = new HashMap<>();
    public final List<String> toolTypes;
    public final boolean customSounds;

    public BlockMechanicFactory(ConfigurationSection section) {
        super(section);
        toolTypes = section.getStringList("tool_types");
        customSounds = OraxenPlugin.get().getConfigsManager().getMechanics().getConfigurationSection("custom_block_sounds").getBoolean("noteblock_and_block", true);

        // this modifier should be executed when all the items have been parsed, just
        // before zipping the pack
        OraxenPlugin.get().getResourcePack().addModifiers(getMechanicID(),
                packFolder -> OraxenPlugin.get().getResourcePack()
                        .writeStringToVirtual("assets/minecraft/blockstates",
                                "mushroom_stem.json", getBlockstateContent()));
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new BlockMechanicListener(this));
    }

    private String getBlockstateContent() {
        JsonObject mushroomStem = new JsonObject();
        JsonArray multipart = new JsonArray();
        // adds default override
        multipart.add(getBlockstateOverride("block/mushroom_stem", 15));
        for (JsonObject override : MUSHROOM_STEM_BLOCKSTATE_OVERRIDES)
            multipart.add(override);
        mushroomStem.add("multipart", multipart);
        return mushroomStem.toString();
    }

    public static JsonObject getBlockstateOverride(String modelName, int when) {
        JsonObject content = new JsonObject();
        JsonObject model = new JsonObject();
        model.addProperty("model", modelName);
        content.add("apply", model);
        content.add("when", BlockMechanic.getBlockstateWhenFields(when));
        return content;
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        BlockMechanic mechanic = new BlockMechanic(this, itemMechanicConfiguration);
        MUSHROOM_STEM_BLOCKSTATE_OVERRIDES
                .add(getBlockstateOverride(mechanic.getModel(itemMechanicConfiguration.getParent().getParent()),
                        mechanic.getCustomVariation()));
        BLOCK_PER_VARIATION.put(mechanic.getCustomVariation(), mechanic);
        addToImplemented(mechanic);
        return mechanic;
    }

    public static BlockMechanic getBlockMechanic(int customVariation) {
        return BLOCK_PER_VARIATION.get(customVariation);
    }

    public static BlockMechanic getBlockMechanic(Block block) {
        return (block.getType() == Material.MUSHROOM_STEM)
                ? BLOCK_PER_VARIATION.get(BlockMechanic.getCode(block)) : null;
    }

    /**
     * Attempts to set the block directly to the model and texture of an Oraxen item.
     *
     * @param block  The block to update.
     * @param itemId The Oraxen item ID.
     */
    public static void setBlockModel(Block block, String itemId) {
        final MechanicFactory mechanicFactory = MechanicsManager.getMechanicFactory("block");
        BlockMechanic blockMechanic = (BlockMechanic) mechanicFactory.getMechanic(itemId);
        MultipleFacing newBlockData = (MultipleFacing) Bukkit.createBlockData(Material.MUSHROOM_STEM);
        BlockMechanic.setBlockFacing(newBlockData, blockMechanic.getCustomVariation());
        block.setBlockData(newBlockData, false);
    }

    /**
     * Attempts to set the block directly to the model and texture of an Oraxen item.
     *
     * @param block             The block to update.
     * @param itemId            The Oraxen item ID.
     * @param blockDataMaterial The material to utilize for block data (Default should be 'MUSHROOM_STEM').
     * @return Whether the process was successful.
     */
    public static boolean setBlockModel(Block block, String itemId, String blockDataMaterial) {
        if (block == null || itemId == null || itemId.isEmpty()) return false;

        final MechanicFactory mechanicFactory = MechanicsManager.getMechanicFactory("block");
        final BlockMechanic blockMechanic = (BlockMechanic) mechanicFactory.getMechanic(itemId);

        Material material;
        if (blockDataMaterial == null || blockDataMaterial.isEmpty()) material = Material.MUSHROOM_STEM;
        else material = Material.getMaterial(blockDataMaterial.toUpperCase().replace(" ", "_").replace("-", "_"));

        final MultipleFacing newBlockData = (MultipleFacing) Bukkit.createBlockData(material);
        BlockMechanic.setBlockFacing(newBlockData, blockMechanic.getCustomVariation());
        block.setBlockData(newBlockData, false);
        return true;
    }


}
