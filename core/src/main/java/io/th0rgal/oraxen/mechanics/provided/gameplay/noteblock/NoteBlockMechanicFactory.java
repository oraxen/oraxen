package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.directional.DirectionalBlock;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.farmblock.FarmBlockTask;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.logstrip.LogStripListener;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import org.apache.commons.lang3.Range;
import org.bukkit.Bukkit;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.configuration.ConfigurationSection;
import team.unnamed.creative.blockstate.BlockState;
import team.unnamed.creative.blockstate.MultiVariant;
import team.unnamed.creative.blockstate.Variant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NoteBlockMechanicFactory extends MechanicFactory {

    public static final Map<Integer, NoteBlockMechanic> BLOCK_PER_VARIATION = new HashMap<>();
    private final Map<String, MultiVariant> variants = new HashMap<>();
    private static NoteBlockMechanicFactory instance;
    public final List<String> toolTypes;
    private boolean farmBlock;
    private static FarmBlockTask farmBlockTask;
    public final int farmBlockCheckDelay;
    public final boolean customSounds;

    public NoteBlockMechanicFactory(ConfigurationSection section) {
        super(section);
        instance = this;
        variants.put("instrument=harp,powered=false", getModelJson("block/note_block"));
        toolTypes = section.getStringList("tool_types");
        farmBlockCheckDelay = section.getInt("farmblock_check_delay");
        farmBlock = false;
        customSounds = OraxenPlugin.get().configsManager().getMechanics().getConfigurationSection("custom_block_sounds").getBoolean("noteblock_and_block", true);

        // this modifier should be executed when all the items have been parsed, just
        // before zipping the pack
        BlockState noteState = OraxenPlugin.get().resourcePack().blockState(Key.key("minecraft:note_block"));
        if (noteState != null) {
            noteState.variants().putAll(variants);
        } else noteState = getBlockState();

        OraxenPlugin.get().resourcePack().blockState(noteState);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(),
                new NoteBlockMechanicListener(),
                new LogStripListener()
        );
        if (customSounds) MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new NoteBlockSoundListener());

        if (VersionUtil.isPaperServer()) {
            MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new NoteBlockMechanicListener.NoteBlockMechanicPaperListener());
            if (!NMSHandlers.isNoteblockUpdatesDisabled()) {
                MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new NoteBlockMechanicListener.NoteBlockMechanicPhysicsListener());
                if (VersionUtil.isSupportedVersionOrNewer("1.20.1")) {
                    Logs.logError("Papers block-updates.disable-noteblock-updates is not enabled.");
                    Logs.logWarning("It is recommended to enable this setting for improved performance and prevent bugs with noteblocks");
                    Logs.logWarning("Otherwise Oraxen needs to listen to very taxing events, which also introduces some bugs");
                    Logs.logWarning("You can enable this setting in ServerFolder/config/paper-global.yml", true);
                }
            }
        }
    }

    public static String getInstrumentName(int id) {
        return switch ((id % 400) / 25) {
            case 1 -> "basedrum";
            case 2 -> "snare";
            case 3 -> "hat";
            case 4 -> "bass";
            case 5 -> "flute";
            case 6 -> "bell";
            case 7 -> "guitar";
            case 8 -> "chime";
            case 9 -> "xylophone";
            case 10 -> "iron_xylophone";
            case 11 -> "cow_bell";
            case 12 -> "didgeridoo";
            case 13 -> "bit";
            case 14 -> "banjo";
            case 15 -> "pling";
            default -> "harp";
        };
    }

    public static MultiVariant getModelJson(String modelName) {
        modelName = modelName.startsWith("minecraft:") ? modelName : "minecraft:" + modelName;
        return MultiVariant.of(Variant.builder().model(Key.key(modelName)).build());
    }

    public static MultiVariant getDirectionalModelJson(String modelName, NoteBlockMechanic mechanic, NoteBlockMechanic parentMechanic) {
        String itemId = mechanic.getItemID();
        DirectionalBlock parent = parentMechanic.getDirectional();
        Variant.Builder variantBuilder = Variant.builder();
        String subBlockModel = mechanic.getDirectional().getDirectionalModel(mechanic);
        subBlockModel = subBlockModel != null ? subBlockModel : modelName;
        subBlockModel = subBlockModel.startsWith("minecraft:") ? subBlockModel : "minecraft:" + subBlockModel;
        variantBuilder.model(Key.key(subBlockModel));
        // If subModel is specified and is different from parent we don't want to rotate it
        if (!Objects.equals(subBlockModel, modelName)) return MultiVariant.of(variantBuilder.build());

        if (Objects.equals(parent.getYBlock(), itemId)) return MultiVariant.of(variantBuilder.build());
        else if (Objects.equals(parent.getXBlock(), itemId)) {
            variantBuilder.x(90);
            //content.addProperty("z", 90);
        } else if (Objects.equals(parent.getZBlock(), itemId)) {
            variantBuilder.x(90);
            variantBuilder.y(90);
        } else if (Objects.equals(parent.getNorthBlock(), itemId))
            return MultiVariant.of(variantBuilder.build());
        else if (Objects.equals(parent.getEastBlock(), itemId)) {
            variantBuilder.y(90);
        } else if (Objects.equals(parent.getSouthBlock(), itemId))
            variantBuilder.y(180);
        else if (Objects.equals(parent.getWestBlock(), itemId)) {
            //content.addProperty("z", 90);
            variantBuilder.y(270);
        } else if (Objects.equals(parent.getUpBlock(), itemId))
            variantBuilder.y(270);
        else if (Objects.equals(parent.getDownBlock(), itemId))
            variantBuilder.x(180);

        return MultiVariant.of(variantBuilder.build());
    }

    public static String getBlockstateVariantName(int id) {
        id += 26;
        return getBlockstateVariantName(getInstrumentName(id), id % 25, id >= 400);
    }

    public static String getBlockstateVariantName(String instrument, int note, boolean powered) {
        return "instrument=" + instrument + ",note=" + note + ",powered=" + powered;
    }

    public static NoteBlockMechanic getBlockMechanic(int customVariation) {
        return BLOCK_PER_VARIATION.get(customVariation);
    }

    public static boolean isEnabled() {
        return instance != null;
    }

    public static NoteBlockMechanicFactory getInstance() {
        return instance;
    }


    /**
     * Attempts to set the block directly to the model and texture of an Oraxen item.
     *
     * @param block  The block to update.
     * @param itemId The Oraxen item ID.
     */
    public static void setBlockModel(Block block, String itemId) {
        final MechanicFactory mechanicFactory = MechanicsManager.getMechanicFactory("noteblock");
        NoteBlockMechanic noteBlockMechanic = (NoteBlockMechanic) mechanicFactory.getMechanic(itemId);
        block.setBlockData(createNoteBlockData(noteBlockMechanic.getCustomVariation()), false);
    }

    private BlockState getBlockState() {
        return BlockState.of(Key.key("minecraft:note_block"), variants);
    }

    @Override
    public Mechanic parse(ConfigurationSection section) {
        NoteBlockMechanic mechanic = new NoteBlockMechanic(this, section);
        if (!Range.between(0, 775).contains(mechanic.getCustomVariation())) {
            Logs.logError("The custom variation of the block " + mechanic.getItemID() + " is not between 0 and 775!");
            Logs.logWarning("The item has failed to build for now to prevent bugs and issues.");
            return null;
        }
        DirectionalBlock directional = mechanic.getDirectional();
        String modelName = mechanic.getModel(section.getParent().getParent());

        if (mechanic.isDirectional() && !directional.isParentBlock()) {
            NoteBlockMechanic parentMechanic = directional.getParentMechanic();
            modelName = (parentMechanic.getModel(section.getParent().getParent()));
            variants.put(getBlockstateVariantName(mechanic.getCustomVariation()),
                    getDirectionalModelJson(modelName, mechanic, parentMechanic));
        } else {
            variants.put(getBlockstateVariantName(mechanic.getCustomVariation()),
                    getModelJson(modelName));
        }

        BLOCK_PER_VARIATION.put(mechanic.getCustomVariation(), mechanic);
        addToImplemented(mechanic);
        return mechanic;
    }

    /**
     * Generate a NoteBlock blockdata from its id
     *
     * @param id The block id.
     */
    @SuppressWarnings("deprecation")
    public static NoteBlock createNoteBlockData(int id) {
        /* We have 16 instruments with 25 notes. All of those blocks can be powered.
         * That's: 16*25*2 = 800 variations. The first 25 variations of PIANO (not powered)
         * will be reserved for the vanilla behavior. We still have 800-25 = 775 variations
         */
        id += 26;
        NoteBlock noteBlock = (NoteBlock) Bukkit.createBlockData(Material.NOTE_BLOCK);
        noteBlock.setInstrument(Instrument.getByType((byte) ((id % 400) / 25)));
        noteBlock.setNote(new Note(id % 25));
        noteBlock.setPowered(id >= 400);
        return noteBlock;
    }

    /**
     * Generate a NoteBlock blockdata from an oraxen id
     *
     * @param itemID The id of an item implementing NoteBlockMechanic
     */
    public NoteBlock createNoteBlockData(String itemID) {
        /* We have 16 instruments with 25 notes. All of those blocks can be powered.
         * That's: 16*25*2 = 800 variations. The first 25 variations of PIANO (not powered)
         * will be reserved for the vanilla behavior. We still have 800-25 = 775 variations
         */
        return createNoteBlockData(((NoteBlockMechanic) getInstance().getMechanic(itemID)).getCustomVariation());
    }

    public void registerFarmBlock() {
        if (farmBlock) return;
        if (farmBlockTask != null) farmBlockTask.cancel();

//        // Dont register if there is no farmblocks in configs
//        List<String> farmblockList = new ArrayList<>();
//        for (ItemBuilder itemBuilder : OraxenItems.getItems()) {
//            String id = OraxenItems.getIdByItem(itemBuilder.build());
//            NoteBlockMechanic mechanic = (NoteBlockMechanic) NoteBlockMechanicFactory.getInstance().getMechanic(id);
//            if (mechanic == null || !mechanic.hasDryout()) continue;
//            farmblockList.add(id);
//        }
//        if (farmblockList.isEmpty()) return;

        farmBlockTask = new FarmBlockTask(farmBlockCheckDelay);

        farmBlockTask.runTaskTimer(OraxenPlugin.get(), 0, farmBlockCheckDelay);
        farmBlock = true;
    }

}
