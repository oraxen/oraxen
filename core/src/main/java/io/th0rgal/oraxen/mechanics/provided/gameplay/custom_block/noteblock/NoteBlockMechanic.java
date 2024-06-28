package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.noteblock;

import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.BreakableMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.CustomBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.noteblock.directional.DirectionalBlock;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.noteblock.logstrip.LogStripping;
import io.th0rgal.oraxen.mechanics.provided.gameplay.storage.StorageMechanic;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.configuration.ConfigurationSection;

public class NoteBlockMechanic extends CustomBlockMechanic {

    private final boolean canIgnite;
    private final boolean isFalling;
    private final boolean beaconBaseBlock;
    private final String instrument;
    private final LogStripping logStripping;
    private final DirectionalBlock directionalBlock;
    private final StorageMechanic storage;

    public NoteBlockMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        // Creates an instance of CustomBlockMechanic and applies the below
        super(mechanicFactory, section);

        canIgnite = section.getBoolean("can_ignite", false);
        isFalling = section.getBoolean("is_falling", false);
        beaconBaseBlock = section.getBoolean("beacon_base_block", false);
        instrument = section.getString("instrument", "block.note_block.bass");

        ConfigurationSection logStripSection = section.getConfigurationSection("log_strip");
        logStripping = logStripSection != null ? new LogStripping(logStripSection) : null;

        ConfigurationSection directionalSection = section.getConfigurationSection("directional");
        directionalBlock = directionalSection != null ? new DirectionalBlock(directionalSection) : null;

        ConfigurationSection storageSection = section.getConfigurationSection("storage");
        storage = storageSection != null ? new StorageMechanic(storageSection) : null;
    }

    @Override
    public NoteBlock createBlockData() {
        Instrument instrument;
        Note note;
        boolean powered;
        if (Settings.LEGACY_NOTEBLOCKS.toBool()) {
            /* We have 16 instruments with 25 notes. All of those blocks can be powered.
             * That's: 16*25*2 = 800 variations. The first 25 variations of PIANO (not powered)
             * will be reserved for the vanilla behavior. We still have 800-25 = 775 variations
             */
            int customVariation = customVariation() + 26;
            instrument = Instrument.getByType((byte) ((customVariation % 400) / 25));
            note = new Note(customVariation % 25);
            powered = customVariation >= 400;
        } else {
            instrument = Instrument.getByType((byte)Math.min(Instrument.values().length, customVariation() / 50));
            note = new Note(customVariation() % 25);
            powered = (customVariation() % 50 >= 25);
        }
        if (instrument == null) return null;

        NoteBlock noteBlock = (NoteBlock) Material.NOTE_BLOCK.createBlockData();
        noteBlock.setInstrument(instrument);
        noteBlock.setNote(note);
        noteBlock.setPowered(powered);

        return noteBlock;
    }

    @Override
    public NoteBlock blockData() {
        return (NoteBlock) super.blockData();
    }

    public boolean isStorage() { return storage != null; }
    public StorageMechanic storage() { return storage; }

    public boolean isLog() {
        NoteBlockMechanic parentMechanic = directionalBlock != null ? directionalBlock.getParentMechanic() : null;
        return parentMechanic != null ? logStripping != null || parentMechanic.isLog() : logStripping != null;
    }
    public LogStripping log() { return logStripping; }

    public boolean isFalling() {
        NoteBlockMechanic parentMechanic = directionalBlock != null ? directionalBlock.getParentMechanic() : null;
        return parentMechanic != null ? isFalling || parentMechanic.isFalling() : isFalling;
    }

    public boolean canIgnite() {
        NoteBlockMechanic parentMechanic = directionalBlock != null ? directionalBlock.getParentMechanic() : null;
        return parentMechanic != null ? canIgnite || parentMechanic.canIgnite() : canIgnite;
    }

    public boolean isBeaconBaseBlock() {
        NoteBlockMechanic parentMechanic = directionalBlock != null ? directionalBlock.getParentMechanic() : null;
        return parentMechanic != null ? beaconBaseBlock || parentMechanic.isBeaconBaseBlock() : beaconBaseBlock;
    }

    public String getInstrument() {
        return instrument;
    }

    public boolean isDirectional() { return directionalBlock != null; }
    public DirectionalBlock directional() { return directionalBlock; }

    @Override
    public BreakableMechanic breakable() {
        NoteBlockMechanic parentMechanic = directionalBlock != null ? directionalBlock.getParentMechanic() : null;
        return parentMechanic != null ? parentMechanic.breakable() : super.breakable();
    }

    @Override
    public boolean hasLight() {
        NoteBlockMechanic parentMechanic = directionalBlock != null ? directionalBlock.getParentMechanic() : null;
        return parentMechanic != null ? super.hasLight() || parentMechanic.hasLight() : super.hasLight();
    }

    @Override
    public BlockSounds blockSounds() {
        NoteBlockMechanic parentMechanic = directionalBlock != null ? directionalBlock.getParentMechanic() : null;
        return parentMechanic == null ? super.blockSounds() : super.blockSounds() == null ? parentMechanic.blockSounds() : super.blockSounds();
    }

    public boolean isInteractable() {
        return hasClickActions() || isStorage();
    }
}
