package io.th0rgal.oraxen.mechanics.provided.gameplay.block;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock.ChorusBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock.ChorusBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockType;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanicFactory;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.block.data.type.Tripwire;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Central lock-free O(1) lookup and registration service for all custom block substrates
 * ({@code NOTE_BLOCK}, {@code TRIPWIRE}, {@code CHORUS_PLANT}, and {@code ShapedBlock} copper variants).
 */
public final class CustomBlockService {

    private static final AtomicReferenceArray<NoteBlockMechanic> NOTE_BLOCKS =
            new AtomicReferenceArray<>(BlockStateCodec.MAX_NOTEBLOCK_VARIATION + 1);
    private static final AtomicReferenceArray<StringBlockMechanic> STRING_BLOCKS =
            new AtomicReferenceArray<>(BlockStateCodec.MAX_TRIPWIRE_VARIATION + 1);
    private static final AtomicReferenceArray<ChorusBlockMechanic> CHORUS_BLOCKS =
            new AtomicReferenceArray<>(BlockStateCodec.MAX_CHORUS_VARIATION + 1);
    private static final Map<Material, ShapedBlockMechanic> SHAPED_BLOCKS = new ConcurrentHashMap<>();

    private CustomBlockService() {
    }

    public static void clearNoteBlocks() {
        for (int i = 0; i < NOTE_BLOCKS.length(); i++) {
            NOTE_BLOCKS.set(i, null);
        }
    }

    public static void clearStringBlocks() {
        for (int i = 0; i < STRING_BLOCKS.length(); i++) {
            STRING_BLOCKS.set(i, null);
        }
    }

    public static void clearChorusBlocks() {
        for (int i = 0; i < CHORUS_BLOCKS.length(); i++) {
            CHORUS_BLOCKS.set(i, null);
        }
    }

    public static void clearShapedBlocks() {
        SHAPED_BLOCKS.clear();
    }

    public static void registerNoteBlock(int variation, @NotNull NoteBlockMechanic mechanic) {
        if (BlockStateCodec.isValidNoteBlockVariation(variation)) {
            NOTE_BLOCKS.set(variation, mechanic);
        }
        NoteBlockMechanicFactory.BLOCK_PER_VARIATION.put(variation, mechanic);
    }

    public static void registerStringBlock(int variation, @NotNull StringBlockMechanic mechanic) {
        if (BlockStateCodec.isValidTripwireVariation(variation)) {
            STRING_BLOCKS.set(variation, mechanic);
        }
        StringBlockMechanicFactory.BLOCK_PER_VARIATION.put(variation, mechanic);
    }

    public static void registerChorusBlock(int variation, @NotNull ChorusBlockMechanic mechanic) {
        if (BlockStateCodec.isValidChorusVariation(variation)) {
            CHORUS_BLOCKS.set(variation, mechanic);
        }
        ChorusBlockMechanicFactory.BLOCK_PER_VARIATION.put(variation, mechanic);
    }

    public static void registerShapedBlock(@NotNull Material material, @NotNull ShapedBlockMechanic mechanic) {
        SHAPED_BLOCKS.put(material, mechanic);
    }

    public static @Nullable NoteBlockMechanic getNoteBlock(int variation) {
        if (BlockStateCodec.isValidNoteBlockVariation(variation)) {
            NoteBlockMechanic fast = NOTE_BLOCKS.get(variation);
            if (fast != null) return fast;
        }
        return NoteBlockMechanicFactory.BLOCK_PER_VARIATION.get(variation);
    }

    public static @Nullable NoteBlockMechanic getNoteBlock(@Nullable BlockData data) {
        if (!NoteBlockMechanicFactory.isEnabled() || !(data instanceof NoteBlock noteBlock)) {
            return null;
        }
        return getNoteBlock(BlockStateCodec.decodeNoteBlockVariation(noteBlock));
    }

    public static @Nullable NoteBlockMechanic getNoteBlock(@Nullable Block block) {
        if (block == null || !NoteBlockMechanicFactory.isEnabled() || block.getType() != Material.NOTE_BLOCK) {
            return null;
        }
        return getNoteBlock(block.getBlockData());
    }

    public static @Nullable StringBlockMechanic getStringBlock(int variation) {
        if (BlockStateCodec.isValidTripwireVariation(variation)) {
            StringBlockMechanic fast = STRING_BLOCKS.get(variation);
            if (fast != null) return fast;
        }
        return StringBlockMechanicFactory.BLOCK_PER_VARIATION.get(variation);
    }

    public static @Nullable StringBlockMechanic getStringBlock(@Nullable BlockData data) {
        if (!StringBlockMechanicFactory.isEnabled() || !(data instanceof Tripwire tripwire)) {
            return null;
        }
        return getStringBlock(BlockStateCodec.decodeTripwireVariation(tripwire));
    }

    public static @Nullable StringBlockMechanic getStringBlock(@Nullable Block block) {
        if (block == null || !StringBlockMechanicFactory.isEnabled() || block.getType() != Material.TRIPWIRE) {
            return null;
        }
        return getStringBlock(block.getBlockData());
    }

    public static @Nullable ChorusBlockMechanic getChorusBlock(int variation) {
        if (BlockStateCodec.isValidChorusVariation(variation)) {
            ChorusBlockMechanic fast = CHORUS_BLOCKS.get(variation);
            if (fast != null) return fast;
        }
        return ChorusBlockMechanicFactory.BLOCK_PER_VARIATION.get(variation);
    }

    public static @Nullable ChorusBlockMechanic getChorusBlock(@Nullable BlockData data) {
        if (!ChorusBlockMechanicFactory.isEnabled()
                || !(data instanceof MultipleFacing multipleFacing)
                || data.getMaterial() != Material.CHORUS_PLANT) {
            return null;
        }
        return getChorusBlock(BlockStateCodec.decodeChorusVariation(multipleFacing));
    }

    public static @Nullable ChorusBlockMechanic getChorusBlock(@Nullable Block block) {
        if (block == null || !ChorusBlockMechanicFactory.isEnabled() || block.getType() != Material.CHORUS_PLANT) {
            return null;
        }
        return getChorusBlock(block.getBlockData());
    }

    public static @Nullable ShapedBlockMechanic getShapedBlock(@Nullable Block block) {
        if (block == null || !ShapedBlockMechanicFactory.isEnabled()
                || ShapedBlockType.fromMaterial(block.getType()) == null) {
            return null;
        }
        BlockMechanicFactory blockFactory = BlockMechanicFactory.getInstance();
        if (blockFactory != null && blockFactory.getShapedBlockFactory() != null) {
            return blockFactory.getShapedBlockFactory().getMechanicFromBlock(block);
        }
        ShapedBlockMechanicFactory direct = ShapedBlockMechanicFactory.getInstance();
        return direct != null ? direct.getMechanicFromBlock(block) : null;
    }

    public static @Nullable Mechanic getCustomBlockMechanic(@Nullable Block block) {
        if (block == null) return null;
        return switch (block.getType()) {
            case NOTE_BLOCK -> getNoteBlock(block);
            case TRIPWIRE -> getStringBlock(block);
            case CHORUS_PLANT -> getChorusBlock(block);
            default -> getShapedBlock(block);
        };
    }

    public static @Nullable Mechanic getCustomBlockMechanic(@Nullable BlockData blockData) {
        if (blockData == null) return null;
        return switch (blockData.getMaterial()) {
            case NOTE_BLOCK -> getNoteBlock(blockData);
            case TRIPWIRE -> getStringBlock(blockData);
            case CHORUS_PLANT -> getChorusBlock(blockData);
            default -> SHAPED_BLOCKS.get(blockData.getMaterial());
        };
    }
}
