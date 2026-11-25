package io.th0rgal.oraxen.commands;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.VersionUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class BlockLimitsCommandTest {
    @Test
    void countsAllocatedSlotsIncludingStackLevelsButNotOldFactoriesOrInvalidIds() {
        MechanicFactory factory = mock(MechanicFactory.class);
        Mechanic current = mock(Mechanic.class);
        Mechanic old = mock(Mechanic.class);
        when(current.getFactory()).thenReturn(factory);
        when(old.getFactory()).thenReturn(mock(MechanicFactory.class));
        Map<Integer, Mechanic> slots = Map.of(0, current, 1, current, 2, current, 3, old, 128, current);

        assertEquals(2, BlockLimitsCommand.countSlots(slots, factory, 1, 127));
        assertEquals(4, BlockLimitsCommand.countSlots(slots, factory, 0, 775));
        assertEquals(0, BlockLimitsCommand.countSlots(slots, null, 1, 127));
    }

    @Test
    void suggestionsOnlyIncludeTypesAvailableOnTheServer() {
        try (var versions = mockStatic(VersionUtil.class)) {
            versions.when(() -> VersionUtil.atOrAbove("1.20.4")).thenReturn(false);
            assertEquals(List.of("note", "chorus", "string"), BlockLimitsCommand.supportedTypes());
            versions.when(() -> VersionUtil.atOrAbove("1.20.4")).thenReturn(true);
            assertEquals(List.of("note", "chorus", "string", "stair", "slab", "door", "trapdoor", "grate", "bulb"),
                    BlockLimitsCommand.supportedTypes());
        }
    }

    @Test
    void acceptsMechanicTypeAndLegacyNamesWithoutCaseSensitivity() {
        assertEquals("note", BlockLimitsCommand.normalizeType("FULL"));
        assertEquals("note", BlockLimitsCommand.normalizeType("note_block"));
        assertEquals("chorus", BlockLimitsCommand.normalizeType("chorusblock"));
        assertEquals("string", BlockLimitsCommand.normalizeType("tripwire"));
        assertEquals("grate", BlockLimitsCommand.normalizeType("GRATE"));
    }

    @Test
    void reportSeparatesTypesAndShowsEmptyAndFullCapacity() {
        String report = PlainTextComponentSerializer.plainText().serialize(BlockLimitsCommand.report(List.of(
                new BlockLimitsCommand.Usage("note", "Note blocks", 776, 0),
                new BlockLimitsCommand.Usage("grate", "Copper grate blocks", 4, 4))));

        assertEquals("Note blocks\n[" + "|".repeat(30) + "]\n0 / 776 used (0.0%) • 776 free\n\n"
                + "Copper grate blocks\n[" + "|".repeat(30) + "]\n4 / 4 used (100.0%) • 0 free", report);
    }

    @Test
    void singleTypeShowsFractionalUsageWithoutExtraBlankLines() {
        String report = PlainTextComponentSerializer.plainText().serialize(BlockLimitsCommand.report(List.of(
                new BlockLimitsCommand.Usage("grate", "Copper grate blocks", 4, 1))));

        assertEquals("Copper grate blocks\n[" + "|".repeat(30) + "]\n1 / 4 used (25.0%) • 3 free", report);
    }
}
