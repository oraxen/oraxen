package io.th0rgal.oraxen.mechanics.provided.gameplay.mushroomstem;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.mushroomstem.logstrip.LogStripListener;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MushroomStemMechanicFactory extends MechanicFactory {

    private static final List<JsonObject> MUSHROOM_STEM_BLOCKSTATE_OVERRIDES = new ArrayList<>();
    private static final Map<Integer, MushroomStemMechanic> BLOCK_PER_VARIATION = new HashMap<>();

    private static MushroomStemMechanicFactory instance;
    public final List<String> toolTypes;

    public MushroomStemMechanicFactory(ConfigurationSection section) {
        super(section);
        toolTypes = section.getStringList("tool_types");

        instance = this;

        // this modifier should be executed when all the items have been parsed, just
        // before zipping the pack
        OraxenPlugin.get().getResourcePack().addModifiers(getMechanicID(),
                packFolder -> OraxenPlugin.get().getResourcePack()
                        .writeStringToVirtual("assets/minecraft/blockstates",
                                "mushroom_stem.json", getBlockstateContent()));
        MechanicsManager.registerListeners(OraxenPlugin.get(), new MushroomStemMechanicListener(this));
        MechanicsManager.registerListeners(OraxenPlugin.get(), new LogStripListener(this));
    }

    public static MushroomStemMechanicFactory getInstance() {
        return instance;
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
        content.add("when", MushroomStemMechanic.getBlockstateWhenFields(when));
        return content;
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        MushroomStemMechanic mechanic = new MushroomStemMechanic(this, itemMechanicConfiguration);
        MUSHROOM_STEM_BLOCKSTATE_OVERRIDES
                .add(getBlockstateOverride(mechanic.getModel(itemMechanicConfiguration.getParent().getParent()),
                        mechanic.getCustomVariation()));
        BLOCK_PER_VARIATION.put(mechanic.getCustomVariation(), mechanic);
        addToImplemented(mechanic);
        return mechanic;
    }

    public static MushroomStemMechanic getMushroomStemMechanic(int customVariation) {
        return BLOCK_PER_VARIATION.get(customVariation);
    }

    public static MushroomStemMechanic getMushroomStemMechanic(Block block) {
        return BLOCK_PER_VARIATION.get(MushroomStemMechanic.getCode(block));
    }

    /**
     * Attempts to set the block directly to the model and texture of an Oraxen item.
     *
     * @param block  The block to update.
     * @param itemId The Oraxen item ID.
     */
    public static void setBlockModel(Block block, String itemId) {
        final MechanicFactory mechanicFactory = MechanicsManager.getMechanicFactory("block");
        MushroomStemMechanic mushroomStemMechanic = (MushroomStemMechanic) mechanicFactory.getMechanic(itemId);
        MultipleFacing newBlockData = (MultipleFacing) Bukkit.createBlockData(Material.MUSHROOM_STEM);
        MushroomStemMechanic.setBlockFacing(newBlockData, mushroomStemMechanic.getCustomVariation());
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
        final MushroomStemMechanic mushroomStemMechanic = (MushroomStemMechanic) mechanicFactory.getMechanic(itemId);

        Material material;
        if (blockDataMaterial == null || blockDataMaterial.isEmpty()) material = Material.MUSHROOM_STEM;
        else material = Material.getMaterial(blockDataMaterial.toUpperCase().replace(" ", "_").replace("-", "_"));

        final MultipleFacing newBlockData = (MultipleFacing) Bukkit.createBlockData(material);
        MushroomStemMechanic.setBlockFacing(newBlockData, mushroomStemMechanic.getCustomVariation());
        block.setBlockData(newBlockData, false);
        return true;
    }


}
