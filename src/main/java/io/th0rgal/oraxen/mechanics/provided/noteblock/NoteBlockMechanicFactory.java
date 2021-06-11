package io.th0rgal.oraxen.mechanics.provided.noteblock;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.pack.generation.ResourcePack;
import io.th0rgal.oraxen.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.block.Block;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NoteBlockMechanicFactory extends MechanicFactory {

    private static final List<JsonObject> BLOCKSTATE_OVERRIDES = new ArrayList<>();
    private static final Map<Integer, NoteBlockMechanic> BLOCK_PER_VARIATION = new HashMap<>();
    public final List<String> toolTypes;

    public NoteBlockMechanicFactory(ConfigurationSection section) {
        super(section);
        toolTypes = section.getStringList("tool_types");

        // this modifier should be executed when all the items have been parsed, just
        // before zipping the pack
        ResourcePack.addModifiers(packFolder -> {
            File blockstatesFolder = new File(packFolder, "blockstates");
            if (!blockstatesFolder.exists())
                blockstatesFolder.mkdirs();
            File file = new File(blockstatesFolder, "note_block.json");
            Utils.writeStringToFile(file, getBlockstateContent());
        });
        MechanicsManager.registerListeners(OraxenPlugin.get(), new NoteBlockMechanicListener(this));
    }

    private String getBlockstateContent() {
        JsonObject noteblock = new JsonObject();
        JsonArray multipart = new JsonArray();
        // adds default override
        multipart.add(getBlockstateOverride("required/note_block", "harp"));
        for (JsonObject override : BLOCKSTATE_OVERRIDES)
            multipart.add(override);
        noteblock.add("multipart", multipart);
        return noteblock.toString();
    }

    public static String getInstrumentName(int id) {
        switch (id / 25 % 384) {
            case 1:
                return "basedrum";
            case 2:
                return "snare";
            case 3:
                return "hat";
            case 4:
                return "bass";
            case 5:
                return "flute";
            case 6:
                return "bell";
            case 7:
                return "guitar";
            case 8:
                return "chime";
            case 9:
                return "xylophone";
            case 10:
                return "iron_xylophone";
            case 11:
                return "cow_bell";
            case 12:
                return "didgeridoo";
            case 13:
                return "bit";
            case 14:
                return "banjo";
            case 15:
                return "pling";
            default:
                return "harp";
        }
    }

    public static JsonObject getBlockstateOverride(String modelName, int id) {
        id += 26;
        return getBlockstateOverride(modelName, getInstrumentName(id), id % 25, id >= 400);
    }

    public static JsonObject getBlockstateOverride(String modelName, String instrument) {
        JsonObject content = new JsonObject();
        JsonObject model = new JsonObject();
        model.addProperty("model", modelName);
        content.add("apply", model);

        JsonObject when = new JsonObject();
        when.addProperty("instrument", instrument);
        content.add("when", when);
        return content;
    }

    public static JsonObject getBlockstateOverride(String modelName, String instrument, int note, boolean powered) {
        JsonObject content = new JsonObject();
        JsonObject model = new JsonObject();
        model.addProperty("model", modelName);
        content.add("apply", model);

        JsonObject when = new JsonObject();
        when.addProperty("instrument", instrument);
        when.addProperty("note", note);
        when.addProperty("powered", powered);
        content.add("when", when);
        return content;
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        NoteBlockMechanic mechanic = new NoteBlockMechanic(this, itemMechanicConfiguration);
        BLOCKSTATE_OVERRIDES
                .add(getBlockstateOverride(mechanic.getModel(itemMechanicConfiguration.getParent().getParent()),
                        mechanic.getCustomVariation()));
        BLOCK_PER_VARIATION.put(mechanic.getCustomVariation(), mechanic);
        addToImplemented(mechanic);
        return mechanic;
    }

    public static NoteBlockMechanic getBlockMechanic(int customVariation) {
        return BLOCK_PER_VARIATION.get(customVariation);
    }

    @SuppressWarnings("deprecation")
    public static NoteBlock createNoteBlockData(int id) {
        /* We have 16 instruments with 25 notes. All of those blocks can be powered.
         * That's: 16*25*2 = 800 variations. The first 25 variations of PIANO (not powered)
         * will be reserved for the vanilla behavior. We still have 800-25 = 775 variations
         */
        id += 26;
        NoteBlock noteBlock = (NoteBlock) Bukkit.createBlockData(Material.NOTE_BLOCK);
        noteBlock.setInstrument(Instrument.getByType((byte) (id / 25 % 400)));
        noteBlock.setNote(new Note(id % 25));
        noteBlock.setPowered(id >= 400);
        return noteBlock;
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

}
