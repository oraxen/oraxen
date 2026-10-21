package io.th0rgal.oraxen.mechanics.provided.gameplay.block;

import io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock.ChorusBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.Tripwire;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomBlockStateCodecAndRegistryTest {

    @BeforeEach
    void setUp() {
        VariationRegistry.resetAllForTests();
        CustomBlockService.clearNoteBlocks();
        CustomBlockService.clearStringBlocks();
        CustomBlockService.clearChorusBlocks();
        CustomBlockService.clearShapedBlocks();
    }

    @Test
    @SuppressWarnings("deprecation")
    void noteBlockCodecDecodesAllValidVariationsLosslessly() {
        // Raw state index = variation + 26 spans [26..799] across 16 instruments * 25 notes * 2 powered states
        for (int variation = BlockStateCodec.MIN_NOTEBLOCK_VARIATION;
             variation < 800 - 26;
             variation++) {
            assertTrue(BlockStateCodec.isValidNoteBlockVariation(variation));
            int raw = variation + 26;
            Instrument instrument = Instrument.getByType((byte) ((raw % 400) / 25));
            Note note = new Note(raw % 25);
            boolean powered = raw >= 400;

            NoteBlock mockBlockData = mock(NoteBlock.class);
            when(mockBlockData.getInstrument()).thenReturn(instrument);
            when(mockBlockData.getNote()).thenReturn(note);
            when(mockBlockData.isPowered()).thenReturn(powered);

            assertEquals(variation, BlockStateCodec.decodeNoteBlockVariation(mockBlockData));
            String key = BlockStateCodec.formatNoteBlockVariantKey(variation);
            assertTrue(key.startsWith("instrument="));
            assertTrue(key.contains(",note=" + (raw % 25) + ",powered=" + powered));
        }
        assertFalse(BlockStateCodec.isValidNoteBlockVariation(-1));
        assertFalse(BlockStateCodec.isValidNoteBlockVariation(776));
    }

    @Test
    void tripwireCodecDecodesAllValidVariationsLosslessly() {
        for (int variation = BlockStateCodec.MIN_TRIPWIRE_VARIATION;
             variation <= BlockStateCodec.MAX_TRIPWIRE_VARIATION;
             variation++) {
            assertTrue(BlockStateCodec.isValidTripwireVariation(variation));
            final int v = variation;
            Tripwire tripwire = mock(Tripwire.class);
            when(tripwire.hasFace(BlockFace.EAST)).thenReturn((v & 0x1) != 0);
            when(tripwire.hasFace(BlockFace.WEST)).thenReturn((v & 0x2) != 0);
            when(tripwire.hasFace(BlockFace.SOUTH)).thenReturn((v & 0x4) != 0);
            when(tripwire.hasFace(BlockFace.NORTH)).thenReturn((v & 0x8) != 0);
            when(tripwire.isAttached()).thenReturn((v & 0x10) != 0);
            when(tripwire.isDisarmed()).thenReturn((v & 0x20) != 0);
            when(tripwire.isPowered()).thenReturn((v & 0x40) != 0);

            assertEquals(variation, BlockStateCodec.decodeTripwireVariation(tripwire));
        }
        assertEquals(
                "east=true,west=false,south=true,north=false,attached=false,disarmed=false,powered=false",
                BlockStateCodec.formatTripwireVariantKey(5)
        );
    }

    @Test
    void chorusCodecDecodesAllValidVariationsLosslessly() {
        for (int variation = BlockStateCodec.MIN_CHORUS_VARIATION;
             variation <= BlockStateCodec.MAX_CHORUS_VARIATION;
             variation++) {
            assertTrue(BlockStateCodec.isValidChorusVariation(variation));
            final int v = variation;
            MultipleFacing chorus = mock(MultipleFacing.class);
            when(chorus.hasFace(BlockFace.NORTH)).thenReturn((v & 0x1) != 0);
            when(chorus.hasFace(BlockFace.SOUTH)).thenReturn((v & 0x2) != 0);
            when(chorus.hasFace(BlockFace.EAST)).thenReturn((v & 0x4) != 0);
            when(chorus.hasFace(BlockFace.WEST)).thenReturn((v & 0x8) != 0);
            when(chorus.hasFace(BlockFace.UP)).thenReturn((v & 0x10) != 0);
            when(chorus.hasFace(BlockFace.DOWN)).thenReturn((v & 0x20) != 0);

            assertEquals(variation, BlockStateCodec.decodeChorusVariation(chorus));
        }
    }

    @Test
    void shapedBlockPoseExtractsAndCopiesStairAndDoorProperties() {
        Stairs sourceStairs = mock(Stairs.class);
        when(sourceStairs.getFacing()).thenReturn(BlockFace.WEST);
        when(sourceStairs.getHalf()).thenReturn(Bisected.Half.TOP);
        when(sourceStairs.getShape()).thenReturn(Stairs.Shape.INNER_LEFT);
        when(sourceStairs.isWaterlogged()).thenReturn(true);

        Stairs targetStairs = mock(Stairs.class);
        when(targetStairs.getFaces()).thenReturn(Set.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST));

        BlockStateCodec.copyPose(sourceStairs, targetStairs);

        verify(targetStairs).setFacing(BlockFace.WEST);
        verify(targetStairs).setHalf(Bisected.Half.TOP);
        verify(targetStairs).setShape(Stairs.Shape.INNER_LEFT);
        verify(targetStairs).setWaterlogged(true);

        Door sourceDoor = mock(Door.class);
        when(sourceDoor.getFacing()).thenReturn(BlockFace.SOUTH);
        when(sourceDoor.getHalf()).thenReturn(Bisected.Half.BOTTOM);
        when(sourceDoor.getHinge()).thenReturn(Door.Hinge.RIGHT);
        when(sourceDoor.isOpen()).thenReturn(true);
        when(sourceDoor.isPowered()).thenReturn(false);

        Door targetDoor = mock(Door.class);
        when(targetDoor.getFaces()).thenReturn(Set.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST));

        BlockStateCodec.copyPose(sourceDoor, targetDoor);

        verify(targetDoor).setFacing(BlockFace.SOUTH);
        verify(targetDoor).setHalf(Bisected.Half.BOTTOM);
        verify(targetDoor).setHinge(Door.Hinge.RIGHT);
        verify(targetDoor).setOpen(true);
        verify(targetDoor).setPowered(false);
    }

    @Test
    void variationRegistryAllocatesNonCollidingVariationsAndPersistsAcrossReloads() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection explicitSec = yaml.createSection("explicit_item");
        explicitSec.set("custom_variation", 1);

        ConfigurationSection autoSec1 = yaml.createSection("auto_item_1");
        ConfigurationSection autoSec2 = yaml.createSection("auto_item_2");
        autoSec2.set("custom_variation", "auto");

        int vExplicit = VariationRegistry.resolveOrAllocate("stringblock", "explicit_item", explicitSec, 1, 127);
        int vAuto1 = VariationRegistry.resolveOrAllocate("stringblock", "auto_item_1", autoSec1, 1, 127);
        int vAuto2 = VariationRegistry.resolveOrAllocate("stringblock", "auto_item_2", autoSec2, 1, 127);

        assertEquals(1, vExplicit);
        assertEquals(2, vAuto1);
        assertEquals(3, vAuto2);

        // Simulate a reload where items are parsed in reverse order: persisted IDs must stay stable!
        VariationRegistry.resetSessionReservations();
        ConfigurationSection reloadAuto2 = yaml.createSection("reload_auto_2");
        ConfigurationSection reloadAuto1 = yaml.createSection("reload_auto_1");

        assertEquals(3, VariationRegistry.resolveOrAllocate("stringblock", "auto_item_2", reloadAuto2, 1, 127));
        assertEquals(2, VariationRegistry.resolveOrAllocate("stringblock", "auto_item_1", reloadAuto1, 1, 127));
    }

    @Test
    void variationRegistryPreReservesExplicitIdsBeforeAutoAllocation() {
        YamlConfiguration itemsYaml = new YamlConfiguration();
        ConfigurationSection autoBlock = itemsYaml.createSection("first_auto_block").createSection("Mechanics").createSection("block");
        autoBlock.set("type", "FULL");
        autoBlock.set("custom_variation", "auto");

        ConfigurationSection explicitBlock = itemsYaml.createSection("second_explicit_block").createSection("Mechanics").createSection("block");
        explicitBlock.set("type", "FULL");
        explicitBlock.set("custom-variation", 1);

        VariationRegistry.preReserveFromConfiguration(itemsYaml);

        int vAuto = VariationRegistry.resolveOrAllocate("noteblock", "first_auto_block", autoBlock, 1, 799);
        int vExplicit = VariationRegistry.resolveOrAllocate("noteblock", "second_explicit_block", explicitBlock, 1, 799);

        assertEquals(1, vExplicit);
        assertEquals(2, vAuto);
    }

    @Test
    void customBlockServiceProvidesLockFreeO1Lookup() {
        NoteBlockMechanic noteMechanic = mock(NoteBlockMechanic.class);
        StringBlockMechanic stringMechanic = mock(StringBlockMechanic.class);
        ChorusBlockMechanic chorusMechanic = mock(ChorusBlockMechanic.class);
        ShapedBlockMechanic shapedMechanic = mock(ShapedBlockMechanic.class);

        CustomBlockService.registerNoteBlock(42, noteMechanic);
        CustomBlockService.registerStringBlock(17, stringMechanic);
        CustomBlockService.registerChorusBlock(9, chorusMechanic);
        CustomBlockService.registerShapedBlock(Material.WAXED_CUT_COPPER_SLAB, shapedMechanic);

        assertSame(noteMechanic, CustomBlockService.getNoteBlock(42));
        assertSame(stringMechanic, CustomBlockService.getStringBlock(17));
        assertSame(chorusMechanic, CustomBlockService.getChorusBlock(9));
    }
}
