package io.th0rgal.oraxen.mechanics.provided.gameplay.block;

import org.bukkit.Bukkit;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Pure codec separating custom block <b>Identity</b> (variation index / backing material)
 * from mutable <b>Pose</b> (facing, bisected half, slab type, stair shape, open/powered/waterlogged).
 */
public final class BlockStateCodec {

    public static final int MIN_NOTEBLOCK_VARIATION = 0;
    public static final int MAX_NOTEBLOCK_VARIATION = 775;
    private static final int NOTEBLOCK_OFFSET = 26;

    public static final int MIN_TRIPWIRE_VARIATION = 1;
    public static final int MAX_TRIPWIRE_VARIATION = 127;

    public static final int MIN_CHORUS_VARIATION = 1;
    public static final int MAX_CHORUS_VARIATION = 63;

    public static final int MIN_SHAPED_VARIATION = 1;
    public static final int MAX_SHAPED_VARIATION = 4;

    private static final BlockFace[] TRIPWIRE_FACES = {
            BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH, BlockFace.NORTH
    };

    private BlockStateCodec() {
    }

    // ==================== NOTEBLOCK CODEC (0..775) ====================

    public static boolean isValidNoteBlockVariation(int variation) {
        return variation >= MIN_NOTEBLOCK_VARIATION && variation <= MAX_NOTEBLOCK_VARIATION;
    }

    public static @NotNull String getNoteBlockInstrumentName(int rawStateIndex) {
        return switch ((rawStateIndex % 400) / 25) {
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

    public static @NotNull String formatNoteBlockVariantKey(int variation) {
        int raw = variation + NOTEBLOCK_OFFSET;
        return formatNoteBlockVariantKey(getNoteBlockInstrumentName(raw), raw % 25, raw >= 400);
    }

    public static @NotNull String formatNoteBlockVariantKey(@NotNull String instrument, int note, boolean powered) {
        return "instrument=" + instrument + ",note=" + note + ",powered=" + powered;
    }

    @SuppressWarnings("deprecation")
    public static int decodeNoteBlockVariation(@NotNull NoteBlock noteBlock) {
        return (noteBlock.getInstrument().getType() * 25)
                + noteBlock.getNote().getId()
                + (noteBlock.isPowered() ? 400 : 0)
                - NOTEBLOCK_OFFSET;
    }

    @SuppressWarnings("deprecation")
    public static @NotNull NoteBlock encodeNoteBlockVariation(int variation) {
        int raw = variation + NOTEBLOCK_OFFSET;
        NoteBlock noteBlock = (NoteBlock) Bukkit.createBlockData(Material.NOTE_BLOCK);
        Instrument instrument = Instrument.getByType((byte) ((raw % 400) / 25));
        if (instrument != null) {
            noteBlock.setInstrument(instrument);
        }
        noteBlock.setNote(new Note(raw % 25));
        noteBlock.setPowered(raw >= 400);
        return noteBlock;
    }

    // ==================== TRIPWIRE / STRINGBLOCK CODEC (1..127) ====================

    public static boolean isValidTripwireVariation(int variation) {
        return variation >= MIN_TRIPWIRE_VARIATION && variation <= MAX_TRIPWIRE_VARIATION;
    }

    public static @NotNull String formatTripwireVariantKey(int variation) {
        return "east=" + ((variation & 0x1) != 0)
                + ",west=" + ((variation & 0x2) != 0)
                + ",south=" + ((variation & 0x4) != 0)
                + ",north=" + ((variation & 0x8) != 0)
                + ",attached=" + ((variation & 0x10) != 0)
                + ",disarmed=" + ((variation & 0x20) != 0)
                + ",powered=" + ((variation & 0x40) != 0);
    }

    public static int decodeTripwireVariation(@NotNull Tripwire tripwire) {
        int code = 0;
        if (tripwire.hasFace(BlockFace.EAST)) code |= 0x1;
        if (tripwire.hasFace(BlockFace.WEST)) code |= 0x2;
        if (tripwire.hasFace(BlockFace.SOUTH)) code |= 0x4;
        if (tripwire.hasFace(BlockFace.NORTH)) code |= 0x8;
        if (tripwire.isAttached()) code |= 0x10;
        if (tripwire.isDisarmed()) code |= 0x20;
        if (tripwire.isPowered()) code |= 0x40;
        return code;
    }

    public static @NotNull Tripwire encodeTripwireVariation(int variation) {
        Tripwire data = (Tripwire) Bukkit.createBlockData(Material.TRIPWIRE);
        for (int i = 0; i < TRIPWIRE_FACES.length; i++) {
            data.setFace(TRIPWIRE_FACES[i], (variation & (1 << i)) != 0);
        }
        data.setAttached((variation & 0x10) != 0);
        data.setDisarmed((variation & 0x20) != 0);
        data.setPowered((variation & 0x40) != 0);
        return data;
    }

    // ==================== CHORUS PLANT CODEC (1..63) ====================

    public static boolean isValidChorusVariation(int variation) {
        return variation >= MIN_CHORUS_VARIATION && variation <= MAX_CHORUS_VARIATION;
    }

    public static @NotNull String formatChorusVariantKey(int variation) {
        return "east=" + ((variation & 0x4) != 0)
                + ",north=" + ((variation & 0x1) != 0)
                + ",south=" + ((variation & 0x2) != 0)
                + ",west=" + ((variation & 0x8) != 0)
                + ",up=" + ((variation & 0x10) != 0)
                + ",down=" + ((variation & 0x20) != 0);
    }

    public static int decodeChorusVariation(@NotNull MultipleFacing data) {
        int code = 0;
        if (data.hasFace(BlockFace.NORTH)) code |= 0x1;
        if (data.hasFace(BlockFace.SOUTH)) code |= 0x2;
        if (data.hasFace(BlockFace.EAST)) code |= 0x4;
        if (data.hasFace(BlockFace.WEST)) code |= 0x8;
        if (data.hasFace(BlockFace.UP)) code |= 0x10;
        if (data.hasFace(BlockFace.DOWN)) code |= 0x20;
        return code;
    }

    public static @NotNull MultipleFacing encodeChorusVariation(int variation) {
        MultipleFacing data = (MultipleFacing) Bukkit.createBlockData(Material.CHORUS_PLANT);
        data.setFace(BlockFace.NORTH, (variation & 0x1) != 0);
        data.setFace(BlockFace.SOUTH, (variation & 0x2) != 0);
        data.setFace(BlockFace.EAST, (variation & 0x4) != 0);
        data.setFace(BlockFace.WEST, (variation & 0x8) != 0);
        data.setFace(BlockFace.UP, (variation & 0x10) != 0);
        data.setFace(BlockFace.DOWN, (variation & 0x20) != 0);
        return data;
    }

    // ==================== SHAPED BLOCK POSE CODEC ====================

    /**
     * Immutable snapshot of a shaped block's mutable pose state (orientation, half, open, waterlogged, etc.),
     * decoupled from its material/variation identity.
     */
    public record ShapedBlockPose(
            @Nullable BlockFace facing,
            @Nullable Bisected.Half half,
            @Nullable Stairs.Shape stairShape,
            @Nullable Slab.Type slabType,
            @Nullable Door.Hinge doorHinge,
            @Nullable Boolean open,
            @Nullable Boolean powered,
            @Nullable Boolean lit,
            @Nullable Boolean waterlogged
    ) {
        public static @NotNull ShapedBlockPose extract(@NotNull BlockData data) {
            BlockFace facing = (data instanceof Directional dir) ? dir.getFacing() : null;
            Bisected.Half half = (data instanceof Bisected bis) ? bis.getHalf() : null;
            Stairs.Shape stairShape = (data instanceof Stairs stairs) ? stairs.getShape() : null;
            Slab.Type slabType = (data instanceof Slab slab) ? slab.getType() : null;
            Door.Hinge doorHinge = (data instanceof Door door) ? door.getHinge() : null;
            Boolean open = (data instanceof Openable openable) ? openable.isOpen() : null;
            Boolean powered = (data instanceof Powerable powerable) ? powerable.isPowered() : null;
            Boolean lit = (data instanceof Lightable lightable) ? lightable.isLit() : null;
            Boolean waterlogged = (data instanceof Waterlogged wl) ? wl.isWaterlogged() : null;
            return new ShapedBlockPose(facing, half, stairShape, slabType, doorHinge, open, powered, lit, waterlogged);
        }

        public void applyTo(@NotNull BlockData target) {
            applyGeometry(target);
            applyFlags(target);
        }

        private void applyGeometry(@NotNull BlockData target) {
            if (facing != null && target instanceof Directional dir && dir.getFaces().contains(facing)) {
                dir.setFacing(facing);
            }
            if (half != null && target instanceof Bisected bis) {
                bis.setHalf(half);
            }
            if (stairShape != null && target instanceof Stairs stairs) {
                stairs.setShape(stairShape);
            }
            if (slabType != null && target instanceof Slab slab) {
                slab.setType(slabType);
            }
            if (doorHinge != null && target instanceof Door door) {
                door.setHinge(doorHinge);
            }
        }

        private void applyFlags(@NotNull BlockData target) {
            if (open != null && target instanceof Openable openable) {
                openable.setOpen(open);
            }
            if (powered != null && target instanceof Powerable powerable) {
                powerable.setPowered(powered);
            }
            if (lit != null && target instanceof Lightable lightable) {
                lightable.setLit(lit);
            }
            if (waterlogged != null && target instanceof Waterlogged wl) {
                wl.setWaterlogged(waterlogged);
            }
        }
    }

    public static void copyPose(@NotNull BlockData source, @NotNull BlockData target) {
        ShapedBlockPose.extract(source).applyTo(target);
    }
}
