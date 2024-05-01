package io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock;

import com.google.gson.JsonObject;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.sapling.SaplingListener;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.sapling.SaplingTask;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.utils.VersionUtil;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StringBlockMechanicFactory extends MechanicFactory {

    public static final Map<Integer, StringBlockMechanic> BLOCK_PER_VARIATION = new HashMap<>();
    private static JsonObject variants;
    private static StringBlockMechanicFactory instance;
    public final List<String> toolTypes;
    private boolean sapling;
    private static SaplingTask saplingTask;
    private final int saplingGrowthCheckDelay;
    public final boolean customSounds;
    public final boolean disableVanillaString;

    public StringBlockMechanicFactory(ConfigurationSection section) {
        super(section);
        instance = this;
        variants = new JsonObject();
        variants.add("east=false,west=false,south=false,north=false,attached=false,disarmed=false,powered=false", getModelJson("block/barrier"));
        toolTypes = section.getStringList("tool_types");
        saplingGrowthCheckDelay = section.getInt("sapling_growth_check_delay");
        sapling = false;
        customSounds = OraxenPlugin.get().getConfigsManager().getMechanics().getConfigurationSection("custom_block_sounds").getBoolean("stringblock_and_furniture", true);
        disableVanillaString = section.getBoolean("disable_vanilla_strings", true);

        // this modifier should be executed when all the items have been parsed, just
        // before zipping the pack
        OraxenPlugin.get().getResourcePack().addModifiers(getMechanicID(),
                packFolder ->
                        OraxenPlugin.get().getResourcePack()
                                .writeStringToVirtual("assets/minecraft/blockstates",
                                        "tripwire.json", getBlockstateContent())
        );
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new StringBlockMechanicListener(), new SaplingListener());
        if (customSounds) MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new StringBlockSoundListener());

        // Physics-related stuff
        if (VersionUtil.isPaperServer())
            MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new StringBlockMechanicListener.StringBlockMechanicPaperListener());
        if (!VersionUtil.isPaperServer() || !NMSHandlers.isTripwireUpdatesDisabled())
            MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new StringBlockMechanicListener.StringBlockMechanicPhysicsListener());
        if (VersionUtil.isPaperServer() && VersionUtil.atOrAbove("1.20.1") && !NMSHandlers.isTripwireUpdatesDisabled()) {
            Logs.logError("Papers block-updates.disable-tripwire-updates is not enabled.");
            Logs.logWarning("It is recommended to enable this setting for improved performance and prevent bugs with tripwires");
            Logs.logWarning("Otherwise Oraxen needs to listen to very taxing events, which also introduces some bugs");
            Logs.logWarning("You can enable this setting in ServerFolder/config/paper-global.yml", true);
        }
    }

    public static JsonObject getModelJson(String modelName) {
        JsonObject content = new JsonObject();
        content.addProperty("model", modelName);
        return content;
    }

    public static String getBlockstateVariantName(int id) {
        return "east=" + ((id & 1) == 1 ? "true" : "false")
                + ",west=" + (((id >> 1) & 1) == 1 ? "true" : "false")
                + ",south=" + (((id >> 2) & 1) == 1 ? "true" : "false")
                + ",north=" + (((id >> 3) & 1) == 1 ? "true" : "false")
                + ",attached=" + (((id >> 4) & 1) == 1 ? "true" : "false")
                + ",disarmed=" + (((id >> 5) & 1) == 1 ? "true" : "false")
                + ",powered=" + (((id >> 6) & 1) == 1 ? "true" : "false");
    }

    @org.jetbrains.annotations.Nullable
    public static StringBlockMechanic getBlockMechanic(int customVariation) {
        return BLOCK_PER_VARIATION.get(customVariation);
    }

    public static boolean isEnabled() {
        return instance != null;
    }

    public static StringBlockMechanicFactory getInstance() {
        return instance;
    }


    /**
     * Attempts to set the block directly to the model and texture of an Oraxen item.
     *
     * @param block  The block to update.
     * @param itemId The Oraxen item ID.
     */
    public static void setBlockModel(Block block, String itemId) {
        final MechanicFactory mechanicFactory = MechanicsManager.getMechanicFactory("stringblock");
        StringBlockMechanic stringBlockMechanic = (StringBlockMechanic) mechanicFactory.getMechanic(itemId);
        block.setBlockData(createTripwireData(stringBlockMechanic.getCustomVariation()));
    }

    private String getBlockstateContent() {
        JsonObject tripwire = new JsonObject();
        tripwire.add("variants", variants);
        return tripwire.toString();
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        StringBlockMechanic mechanic = new StringBlockMechanic(this, itemMechanicConfiguration);
        if (!Range.between(1, 127).contains(mechanic.getCustomVariation())) {
            Logs.logError("The custom variation of the block " + mechanic.getItemID() + " is not between 1 and 127!");
            Logs.logWarning("The item has failed to build for now to prevent bugs and issues.");
        }
        variants.add(getBlockstateVariantName(mechanic.getCustomVariation()),
                getModelJson(mechanic.getModel(itemMechanicConfiguration.getParent()
                        .getParent())));
        BLOCK_PER_VARIATION.put(mechanic.getCustomVariation(), mechanic);
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
        final List<BlockFace> properties = Arrays
                .asList(BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH, BlockFace.NORTH);
        int sum = 0;
        for (final BlockFace blockFace : blockData.getFaces())
            sum += (int) Math.pow(2, properties.indexOf(blockFace));
        if (blockData.isAttached())
            sum += (int) Math.pow(2, 4);
        if (blockData.isDisarmed())
            sum += (int) Math.pow(2, 5);
        if (blockData.isPowered())
            sum += (int) Math.pow(2, 6);
        return sum;
    }

    /**
     * Generate a Tripwire blockdata from its id
     *
     * @param code The block id.
     */
    public static BlockData createTripwireData(final int code) {
        Tripwire data = ((Tripwire) Bukkit.createBlockData(Material.TRIPWIRE));
        int i = 0;
        for (BlockFace face : new BlockFace[]{BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH,
                BlockFace.NORTH})
            data.setFace(face, (code & 0x1 << i++) != 0);
        data.setAttached((code & 0x1 << i++) != 0);
        data.setDisarmed((code & 0x1 << i++) != 0);
        data.setPowered((code & 0x1 << i) != 0);
        return data;
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

        // Disabled for abit as OraxenItems.getItems() here
        // Dont register if there is no sapling in configs
//        List<String> saplingList = new ArrayList<>();
//        for (ItemBuilder itemBuilder : OraxenItems.getItems()) {
//            String id = OraxenItems.getIdByItem(itemBuilder.build());
//            StringBlockMechanic mechanic = (StringBlockMechanic) StringBlockMechanicFactory.getInstance().getMechanic(id);
//            if (mechanic == null || !mechanic.isSapling()) continue;
//            saplingList.add(id);
//        }
//        if (saplingList.isEmpty()) return;

        saplingTask = new SaplingTask(saplingGrowthCheckDelay);
        saplingTask.runTaskTimer(OraxenPlugin.get(), 0, saplingGrowthCheckDelay);
        sapling = true;
    }
}
