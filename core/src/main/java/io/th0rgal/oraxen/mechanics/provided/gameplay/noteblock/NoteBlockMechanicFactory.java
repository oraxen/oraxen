package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.directional.DirectionalBlock;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.logstrip.LogStripListener;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import org.apache.commons.lang3.Range;
import org.bukkit.Instrument;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.unnamed.creative.blockstate.BlockState;
import team.unnamed.creative.blockstate.MultiVariant;
import team.unnamed.creative.blockstate.Variant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NoteBlockMechanicFactory extends MechanicFactory {

    private static final Integer MAX_PER_INSTRUMENT = 50;
    public static final Integer MAX_BLOCK_VARIATION = Instrument.values().length * MAX_PER_INSTRUMENT - 1;
    public static final Map<Integer, NoteBlockMechanic> BLOCK_PER_VARIATION = new HashMap<>();
    private final Map<String, MultiVariant> variants = new HashMap<>();
    private static NoteBlockMechanicFactory instance;
    public final List<String> toolTypes;
    public final boolean customSounds;
    private final boolean removeMineableTag;

    public NoteBlockMechanicFactory(ConfigurationSection section) {
        super(section);
        instance = this;
        variants.put("instrument=harp,powered=false,note=0", MultiVariant.of(Variant.builder().model(Key.key("block/note_block")).build()));
        toolTypes = section.getStringList("tool_types");
        customSounds = OraxenPlugin.get().configsManager().getMechanics().getConfigurationSection("custom_block_sounds").getBoolean("noteblock_and_block", true);
        removeMineableTag = section.getBoolean("remove_mineable_tag", false);

        BlockState noteState = OraxenPlugin.get().packGenerator().resourcePack().blockState(Key.key("minecraft:note_block"));
        if (noteState != null) noteState.variants().putAll(variants);
        else noteState = BlockState.of(Key.key("minecraft:note_block"), variants);

        OraxenPlugin.get().packGenerator().resourcePack().blockState(noteState);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(),
                new NoteBlockMechanicListener(),
                new LogStripListener());
        if (customSounds) MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new NoteBlockSoundListener());

        // Physics-related stuff
        if (VersionUtil.isPaperServer())
            MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new NoteBlockMechanicListener.NoteBlockMechanicPaperListener());
        if (!VersionUtil.isPaperServer() || !NMSHandlers.isNoteblockUpdatesDisabled())
            MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new NoteBlockMechanicListener.NoteBlockMechanicPhysicsListener());
        if (VersionUtil.isPaperServer() && VersionUtil.atOrAbove("1.20.1") && !NMSHandlers.isNoteblockUpdatesDisabled()) {
            Logs.logError("Papers block-updates.disable-noteblock-updates is not enabled.");
            Logs.logWarning("It is recommended to enable this setting for improved performance and prevent bugs with noteblocks");
            Logs.logWarning("Otherwise Oraxen needs to listen to very taxing events, which also introduces some bugs");
            Logs.logWarning("You can enable this setting in ServerFolder/config/paper-global.yml", true);
        }
    }

    @Nullable
    public static NoteBlockMechanic getBlockMechanic(@NotNull NoteBlock blockData) {
        return BLOCK_PER_VARIATION.values().stream().filter(m -> m.blockData().equals(blockData)).findFirst().orElse(null);
    }

    public static boolean isEnabled() {
        return instance != null;
    }

    public static NoteBlockMechanicFactory getInstance() {
        return instance;
    }

    public boolean removeMineableTag() {
        return removeMineableTag;
    }


    /**
     * Attempts to set the block directly to the model and texture of an Oraxen item.
     *
     * @param block  The block to update.
     * @param itemId The Oraxen item ID.
     */
    public static void setBlockModel(Block block, String itemId) {
        NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(itemId);
        if (mechanic != null) block.setBlockData(mechanic.blockData());
    }

    @Override
    public NoteBlockMechanic getMechanic(String itemID) {
        return (NoteBlockMechanic) super.getMechanic(itemID);
    }

    @Override
    public NoteBlockMechanic getMechanic(ItemStack itemStack) {
        return (NoteBlockMechanic) super.getMechanic(itemStack);
    }

    @Override
    public Mechanic parse(ConfigurationSection section) {
        NoteBlockMechanic mechanic = new NoteBlockMechanic(this, section);
        if (!Range.between(1, MAX_BLOCK_VARIATION).contains(mechanic.getCustomVariation())) {
            Logs.logError("The custom variation of the block " + mechanic.getItemID() + " is not between 1 and " + MAX_BLOCK_VARIATION + "!");
            Logs.logWarning("The item has failed to build for now to prevent bugs and issues.");
            return null;
        }
        DirectionalBlock directional = mechanic.getDirectional();
        Key modelKey = mechanic.getModel();

        String variantName = "instrument=" + instrumentName(mechanic.blockData().getInstrument()) + ",note=" + mechanic.blockData().getNote().getId() + ",powered=" + mechanic.blockData().isPowered();
        if (mechanic.isDirectional() && !directional.isParentBlock()) {
            NoteBlockMechanic parentMechanic = directional.getParentMechanic();
            modelKey = (parentMechanic.getModel());
            variants.put(variantName, getDirectionalModelJson(modelKey, mechanic, parentMechanic));
        } else {
            MultiVariant multiVariant = MultiVariant.of(Variant.builder().model(modelKey).build());
            variants.put(variantName, multiVariant);
        }

        BLOCK_PER_VARIATION.put(mechanic.getCustomVariation(), mechanic);
        addToImplemented(mechanic);
        return mechanic;
    }

    private String instrumentName(Instrument instrument) {
        return switch (instrument) {
            case BASS_DRUM -> "basedrum";
            case PIANO -> "harp";
            case SNARE_DRUM -> "snare";
            case STICKS -> "hat";
            case BASS_GUITAR -> "bass";
            default -> instrument.name().toLowerCase();
        };
    }

    private MultiVariant getDirectionalModelJson(Key modelKey, NoteBlockMechanic mechanic, NoteBlockMechanic parentMechanic) {
        String itemId = mechanic.getItemID();
        DirectionalBlock parent = parentMechanic.getDirectional();
        Variant.Builder variantBuilder = Variant.builder();
        Key subBlockModel = mechanic.getDirectional().getDirectionalModel(mechanic);
        subBlockModel = subBlockModel != null ? subBlockModel : modelKey;
        variantBuilder.model(subBlockModel);
        // If subModel is specified and is different from parent we don't want to rotate it
        if (!Objects.equals(subBlockModel, modelKey)) return MultiVariant.of(variantBuilder.build());

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

}
