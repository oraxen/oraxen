package io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock;

import com.google.gson.JsonObject;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicConfigProperty;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockStateCodec;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.CustomBlockService;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.sapling.SaplingListener;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.sapling.SaplingTask;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.utils.PaperConfigUpdater;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.commons.lang3.Range;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Tripwire;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StringBlockMechanicFactory extends MechanicFactory {

    // Repopulated on reload (a region/command thread on Folia) while every block
    // event handler reads it from other region threads.
    public static final Map<Integer, StringBlockMechanic> BLOCK_PER_VARIATION = new ConcurrentHashMap<>();
    private static JsonObject variants;
    private static StringBlockMechanicFactory instance;
    public final List<String> toolTypes;
    private boolean sapling;
    private static SaplingTask saplingTask;
    private final int saplingGrowthCheckDelay;
    public final boolean customSounds;
    public final boolean disableVanillaString;
    private final boolean registerListeners;
    private boolean enabled;

    public StringBlockMechanicFactory(ConfigurationSection section) {
        this(section, true);
    }

    public StringBlockMechanicFactory(ConfigurationSection section, boolean registerListeners) {
        super(section);
        instance = this;
        BLOCK_PER_VARIATION.clear();
        CustomBlockService.clearStringBlocks();
        variants = new JsonObject();
        variants.add("east=false,west=false,south=false,north=false,attached=false,disarmed=false,powered=false", getModelJson("block/barrier"));
        toolTypes = section.getStringList("tool_types");
        saplingGrowthCheckDelay = section.getInt("sapling_growth_check_delay");
        sapling = false;
        customSounds = areCustomSoundsEnabled();
        disableVanillaString = section.getBoolean("disable_vanilla_strings", true);
        this.registerListeners = registerListeners;
        enabled = false;
    }

    public static JsonObject getModelJson(String modelName) {
        JsonObject content = new JsonObject();
        content.addProperty("model", modelName);
        return content;
    }

    public static String getBlockstateVariantName(int id) {
        return BlockStateCodec.formatTripwireVariantKey(id);
    }

    @org.jetbrains.annotations.Nullable
    public static StringBlockMechanic getBlockMechanic(int customVariation) {
        return CustomBlockService.getStringBlock(customVariation);
    }

    public static boolean isEnabled() {
        return instance != null && instance.enabled && MechanicsManager.isMechanicEnabled("block");
    }

    public static boolean areCustomSoundsEnabled() {
        OraxenPlugin plugin = OraxenPlugin.get();
        if (plugin == null || plugin.getConfigsManager() == null) return true;

        ConfigurationSection customSoundsSection = plugin.getConfigsManager().getMechanics()
                .getConfigurationSection("custom_block_sounds");
        return BlockSounds.isStringBlockSoundEnabled(customSoundsSection);
    }

    public static StringBlockMechanicFactory getInstance() {
        return instance;
    }

    public static void clearInstance(StringBlockMechanicFactory factory) {
        if (instance != factory) return;
        instance = null;
        BLOCK_PER_VARIATION.clear();
        CustomBlockService.clearStringBlocks();
    }


    /**
     * Attempts to set the block directly to the model and texture of an Oraxen item.
     *
     * @param block  The block to update.
     * @param itemId The Oraxen item ID.
     */
    public static void setBlockModel(Block block, String itemId) {
        StringBlockMechanic stringBlockMechanic = getInstance().getMechanic(itemId);
        block.setBlockData(createTripwireData(stringBlockMechanic.getCustomVariation()));
    }

    private String getBlockstateContent() {
        JsonObject tripwire = new JsonObject();
        tripwire.add("variants", variants);
        return tripwire.toString();
    }

    private void enable() {
        if (enabled) return;
        enabled = true;

        // this modifier should be executed when all the items have been parsed, just
        // before zipping the pack
        OraxenPlugin.get().getResourcePack().addModifiers(getMechanicID(),
                packFolder ->
                        OraxenPlugin.get().getResourcePack()
                                .writeStringToVirtual("assets/minecraft/blockstates",
                                        "tripwire.json", getBlockstateContent())
        );
        if (!registerListeners) return;

        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new StringBlockMechanicListener(), new SaplingListener());
        if (customSounds) MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new StringBlockSoundListener());

        // Physics-related stuff
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new StringBlockMechanicListener.StringBlockMechanicPaperListener());
        boolean tripwireUpdatesDisabled = NMSHandlers.isTripwireUpdatesDisabled();
        if (!tripwireUpdatesDisabled)
            MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new StringBlockMechanicListener.StringBlockMechanicPhysicsListener());
        // Warn if Paper config is not set (auto-update happens earlier in plugin enable)
        if (!tripwireUpdatesDisabled
                && PaperConfigUpdater.wasBlockUpdateSettingUpdated("disable-tripwire-updates")) {
            Logs.logWarning("Paper block-updates.disable-tripwire-updates is not enabled, restart may be required");
        }
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        enable();
        StringBlockMechanic mechanic = new StringBlockMechanic(this, itemMechanicConfiguration);
        if (!Range.between(1, 127).contains(mechanic.getCustomVariation())) {
            Logs.logError("The custom_variation of " + mechanic.getItemID() + " is " + mechanic.getCustomVariation() + ", but must be between 1 and 127!");
            Logs.logWarning("The item has failed to build for now to prevent bugs and issues.");
            return null;
        }
        variants.add(getBlockstateVariantName(mechanic.getCustomVariation()),
                getModelJson(mechanic.getModel(itemMechanicConfiguration.getParent()
                        .getParent())));
        StringBlockMechanic previous = CustomBlockService.registerStringBlock(mechanic.getCustomVariation(), mechanic);
        BlockMechanicFactory.logDuplicateVariation(previous, mechanic, mechanic.getCustomVariation());

        // Register stackable variations: each stack level gets its own blockstate entry
        // but maps back to the parent mechanic for unified drop/interaction handling
        for (StringBlockMechanic.StackVariation sv : mechanic.getStackVariations()) {
            if (!Range.between(1, 127).contains(sv.customVariation())) {
                Logs.logError("Stackable custom_variation " + sv.customVariation() + " of " + mechanic.getItemID() + " must be between 1 and 127!");
                continue;
            }
            StringBlockMechanic existing = BLOCK_PER_VARIATION.get(sv.customVariation());
            if (existing != null) {
                Logs.logError("Stackable custom_variation " + sv.customVariation() + " of " + mechanic.getItemID() + " is already used by " + existing.getItemID() + "!");
                continue;
            }
            String stackModel = sv.model() != null ? sv.model()
                    : mechanic.getModel(itemMechanicConfiguration.getParent().getParent());
            variants.add(getBlockstateVariantName(sv.customVariation()), getModelJson(stackModel));
            // Map stack variations back to the parent mechanic
            CustomBlockService.registerStringBlock(sv.customVariation(), mechanic);
        }

        addToImplemented(mechanic);
        return mechanic;
    }

    @Override
    public StringBlockMechanic getMechanic(String itemID) {
        return (StringBlockMechanic) super.getMechanic(itemID);
    }

    @Override
    public StringBlockMechanic getMechanic(ItemStack itemStack) {
        return (StringBlockMechanic) super.getMechanic(itemStack);
    }

    public static int getCode(final Tripwire blockData) {
        return BlockStateCodec.decodeTripwireVariation(blockData);
    }

    /**
     * Generate a Tripwire blockdata from its id
     *
     * @param code The block id.
     */
    public static BlockData createTripwireData(final int code) {
        return BlockStateCodec.encodeTripwireVariation(code);
    }

    /**
     * Generate a NoteBlock blockdata from an oraxen id
     *
     * @param itemID The id of an item implementing NoteBlockMechanic
     */
    public BlockData createTripwireData(String itemID) {
        /* We have 16 instruments with 25 notes. All of those blocks can be powered.
         * That's: 16*25*2 = 800 variations. The first 25 variations of PIANO (not powered)
         * will be reserved for the vanilla behavior. We still have 800-25 = 775 variations
         */
        return createTripwireData(((StringBlockMechanic) getInstance().getMechanic(itemID)).getCustomVariation());
    }

    public void registerSaplingMechanic() {
        if (sapling) return;
        if (saplingTask != null) saplingTask.cancel();

        saplingTask = new SaplingTask(saplingGrowthCheckDelay);
        MechanicsManager.registerTask(getMechanicID(), saplingTask.start(0, saplingGrowthCheckDelay));
        sapling = true;
    }

    @Override
    public @Nullable String getMechanicCategory() {
        return "gameplay";
    }

    @Override
    public @Nullable String getMechanicDescription() {
        return "Creates custom blocks using tripwire states (up to 127 variations)";
    }

    @Override
    public @NotNull List<MechanicConfigProperty> getConfigSchema() {
        return List.of(
                MechanicConfigProperty.integer("custom_variation", "Unique variation ID (1-127)", 1, 1, 127),
                MechanicConfigProperty.string("model", "Block model path"),
                MechanicConfigProperty.decimal("hardness", "Block break hardness", 1.0, 0.0),
                MechanicConfigProperty.integer("light", "Light level emitted (0-15)", 0, 0, 15),
                MechanicConfigProperty.bool("blast_resistant", "Whether block resists explosions", false),
                MechanicConfigProperty.bool("immovable", "Whether block resists pistons", false),
                MechanicConfigProperty.object("drop", "Drop configuration when broken", Map.of(
                        "silktouch", MechanicConfigProperty.bool("silktouch", "Require silk touch", false),
                        "loots", MechanicConfigProperty.list("loots", "List of loot entries")
                )),
                MechanicConfigProperty.object("sapling", "Sapling growth configuration", Map.ofEntries(
                        Map.entry("canGrowNaturally", MechanicConfigProperty.bool("canGrowNaturally", "Whether the sapling grows over time", true)),
                        Map.entry("naturalGrowthTime", MechanicConfigProperty.integer("naturalGrowthTime", "Ticks until the sapling grows", 6000, 0)),
                        Map.entry("canGrowFromBoneMeal", MechanicConfigProperty.bool("canGrowFromBoneMeal", "Whether bone meal speeds up growth", true)),
                        Map.entry("boneMealGrowthSpeedup", MechanicConfigProperty.integer("boneMealGrowthSpeedup", "Ticks of growth each bone meal adds", 1200, 0)),
                        Map.entry("growSound", MechanicConfigProperty.string("growSound", "Sound played when the sapling grows")),
                        Map.entry("minLightLevel", MechanicConfigProperty.integer("minLightLevel", "Minimum light level to grow", 0, 0, 15)),
                        Map.entry("requiresWaterSource", MechanicConfigProperty.bool("requiresWaterSource", "Whether the sapling needs water below it", false)),
                        Map.entry("schematicName", MechanicConfigProperty.string("schematicName", "Schematic pasted when the sapling grows")),
                        Map.entry("replaceBlocks", MechanicConfigProperty.bool("replaceBlocks", "Whether the schematic may replace existing blocks", false)),
                        Map.entry("shouldCopyBiomes", MechanicConfigProperty.bool("shouldCopyBiomes", "Whether the schematic pastes its biomes", false)),
                        Map.entry("shouldCopyEntities", MechanicConfigProperty.bool("shouldCopyEntities", "Whether the schematic pastes its entities", false))
                )),
                MechanicConfigProperty.list("stackable", "List of stack variations (custom_variation + model per level)")
        );
    }
}
