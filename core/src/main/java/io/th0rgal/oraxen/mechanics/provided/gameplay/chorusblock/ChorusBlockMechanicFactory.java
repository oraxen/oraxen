package io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock;

import com.google.gson.JsonObject;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicConfigProperty;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.commons.lang3.Range;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChorusBlockMechanicFactory extends MechanicFactory {

    public static final int MAX_BLOCK_VARIATION = 63;
    public static final Map<Integer, ChorusBlockMechanic> BLOCK_PER_VARIATION = new HashMap<>();
    private static JsonObject variants;
    private static ChorusBlockMechanicFactory instance;
    public final List<String> toolTypes;
    public final boolean customSounds;

    public ChorusBlockMechanicFactory(ConfigurationSection section) {
        super(section);
        instance = this;
        variants = new JsonObject();
        // Reserve variation 0 (all faces false) for vanilla behavior
        variants.add("east=false,north=false,south=false,west=false,up=false,down=false",
                getModelJson("block/chorus_plant"));
        toolTypes = section.getStringList("tool_types");
        customSounds = OraxenPlugin.get().getConfigsManager().getMechanics()
                .getConfigurationSection("custom_block_sounds").getBoolean("chorusblock", true);

        // Register blockstate modifier for chorus_plant.json
        OraxenPlugin.get().getResourcePack().addModifiers(getMechanicID(),
                packFolder -> OraxenPlugin.get().getResourcePack()
                        .writeStringToVirtual("assets/minecraft/blockstates",
                                "chorus_plant.json", getBlockstateContent()));

        // Register listeners
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(),
                new ChorusBlockMechanicListener());
        if (customSounds) {
            MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(),
                    new ChorusBlockSoundListener());
        }

        // Physics-related listeners
        if (VersionUtil.isPaperServer()) {
            MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(),
                    new ChorusBlockMechanicListener.ChorusBlockMechanicPaperListener());
        }
        if (!VersionUtil.isPaperServer() || !NMSHandlers.isChorusPlantUpdatesDisabled()) {
            MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(),
                    new ChorusBlockMechanicListener.ChorusBlockMechanicPhysicsListener());
        }

        // Warn if Paper config is not set
        if (VersionUtil.isPaperServer() && VersionUtil.atOrAbove("1.20.1")
                && !NMSHandlers.isChorusPlantUpdatesDisabled()) {
            Logs.logError("Paper's block-updates.disable-chorus-plant-updates is not enabled.");
            Logs.logWarning("It is recommended to enable this setting for improved performance and prevent bugs with chorus plants");
            Logs.logWarning("Otherwise Oraxen needs to listen to very taxing events, which also introduces some bugs");
            Logs.logWarning("You can enable this setting in ServerFolder/config/paper-global.yml", true);
        }
    }

    public static JsonObject getModelJson(String modelName) {
        JsonObject content = new JsonObject();
        content.addProperty("model", modelName);
        return content;
    }

    /**
     * Gets the blockstate variant name for a given variation ID.
     * Uses bit encoding: NORTH(0) | SOUTH(1) | EAST(2) | WEST(3) | UP(4) | DOWN(5)
     */
    public static String getBlockstateVariantName(int id) {
        return "east=" + ((id & 0x4) != 0)
                + ",north=" + ((id & 0x1) != 0)
                + ",south=" + ((id & 0x2) != 0)
                + ",west=" + ((id & 0x8) != 0)
                + ",up=" + ((id & 0x10) != 0)
                + ",down=" + ((id & 0x20) != 0);
    }

    @Nullable
    public static ChorusBlockMechanic getBlockMechanic(int customVariation) {
        return BLOCK_PER_VARIATION.get(customVariation);
    }

    public static boolean isEnabled() {
        return instance != null;
    }

    public static ChorusBlockMechanicFactory getInstance() {
        return instance;
    }

    /**
     * Create a MultipleFacing blockdata from a variation code.
     * Bit encoding: NORTH(0x1) | SOUTH(0x2) | EAST(0x4) | WEST(0x8) | UP(0x10) | DOWN(0x20)
     */
    public static MultipleFacing createChorusData(int code) {
        MultipleFacing data = (MultipleFacing) Bukkit.createBlockData(Material.CHORUS_PLANT);
        data.setFace(BlockFace.NORTH, (code & 0x1) != 0);
        data.setFace(BlockFace.SOUTH, (code & 0x2) != 0);
        data.setFace(BlockFace.EAST, (code & 0x4) != 0);
        data.setFace(BlockFace.WEST, (code & 0x8) != 0);
        data.setFace(BlockFace.UP, (code & 0x10) != 0);
        data.setFace(BlockFace.DOWN, (code & 0x20) != 0);
        return data;
    }

    /**
     * Get the variation code from a MultipleFacing blockdata.
     */
    public static int getCode(MultipleFacing data) {
        int code = 0;
        if (data.hasFace(BlockFace.NORTH)) code |= 0x1;
        if (data.hasFace(BlockFace.SOUTH)) code |= 0x2;
        if (data.hasFace(BlockFace.EAST)) code |= 0x4;
        if (data.hasFace(BlockFace.WEST)) code |= 0x8;
        if (data.hasFace(BlockFace.UP)) code |= 0x10;
        if (data.hasFace(BlockFace.DOWN)) code |= 0x20;
        return code;
    }

    /**
     * Attempts to set the block directly to the model and texture of an Oraxen item.
     *
     * @param block  The block to update.
     * @param itemId The Oraxen item ID.
     */
    public static void setBlockModel(Block block, String itemId) {
        ChorusBlockMechanic mechanic = getInstance().getMechanic(itemId);
        if (mechanic != null) {
            block.setBlockData(createChorusData(mechanic.getCustomVariation()));
        }
    }

    private String getBlockstateContent() {
        JsonObject chorusPlant = new JsonObject();
        chorusPlant.add("variants", variants);
        return chorusPlant.toString();
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        ChorusBlockMechanic mechanic = new ChorusBlockMechanic(this, itemMechanicConfiguration);
        if (!Range.between(1, MAX_BLOCK_VARIATION).contains(mechanic.getCustomVariation())) {
            Logs.logError("The custom variation of the block " + mechanic.getItemID()
                    + " is not between 1 and " + MAX_BLOCK_VARIATION + "!");
            Logs.logWarning("The item has failed to build for now to prevent bugs and issues.");
            return null;
        }
        variants.add(getBlockstateVariantName(mechanic.getCustomVariation()),
                getModelJson(mechanic.getModel(itemMechanicConfiguration.getParent().getParent())));
        BLOCK_PER_VARIATION.put(mechanic.getCustomVariation(), mechanic);
        addToImplemented(mechanic);
        return mechanic;
    }

    @Override
    public ChorusBlockMechanic getMechanic(String itemID) {
        return (ChorusBlockMechanic) super.getMechanic(itemID);
    }

    @Override
    public ChorusBlockMechanic getMechanic(ItemStack itemStack) {
        return (ChorusBlockMechanic) super.getMechanic(itemStack);
    }

    /**
     * Generate a chorus plant blockdata from an Oraxen item ID.
     *
     * @param itemID The id of an item implementing ChorusBlockMechanic
     */
    public BlockData createChorusData(String itemID) {
        ChorusBlockMechanic mechanic = getMechanic(itemID);
        return mechanic != null ? createChorusData(mechanic.getCustomVariation()) : null;
    }

    @Override
    public @Nullable String getMechanicCategory() {
        return "gameplay";
    }

    @Override
    public @Nullable String getMechanicDescription() {
        return "Creates custom blocks using chorus plant states (up to 63 variations)";
    }

    @Override
    public @NotNull List<MechanicConfigProperty> getConfigSchema() {
        return List.of(
                MechanicConfigProperty.integer("custom_variation", "Unique variation ID (1-63)", 1, 1, 63),
                MechanicConfigProperty.string("model", "Block model path"),
                MechanicConfigProperty.decimal("hardness", "Block break hardness", 1.0, 0.0),
                MechanicConfigProperty.integer("light", "Light level emitted (0-15)", 0, 0, 15),
                MechanicConfigProperty.bool("is_falling", "Whether the block falls like sand", false),
                MechanicConfigProperty.bool("blast_resistant", "Whether the block resists explosions", false),
                MechanicConfigProperty.bool("immovable", "Whether the block can be moved by pistons", false),
                MechanicConfigProperty.object("drop", "Drop configuration when broken", Map.of(
                        "silktouch", MechanicConfigProperty.bool("silktouch", "Require silk touch", false),
                        "loots", MechanicConfigProperty.list("loots", "List of loot entries")
                )),
                MechanicConfigProperty.object("block_sounds", "Custom block sounds", Map.of(
                        "place_sound", MechanicConfigProperty.string("place_sound", "Sound when placed"),
                        "break_sound", MechanicConfigProperty.string("break_sound", "Sound when broken"),
                        "step_sound", MechanicConfigProperty.string("step_sound", "Sound when stepped on"),
                        "hit_sound", MechanicConfigProperty.string("hit_sound", "Sound when hit"),
                        "fall_sound", MechanicConfigProperty.string("fall_sound", "Sound when fallen on")
                )),
                MechanicConfigProperty.object("limited_placing", "Placement restrictions", Map.of(
                        "type", MechanicConfigProperty.string("type", "ALLOW or DENY list type"),
                        "block_types", MechanicConfigProperty.list("block_types", "Block types to allow/deny")
                )),
                MechanicConfigProperty.list("clickActions", "Actions to perform on click")
        );
    }
}
