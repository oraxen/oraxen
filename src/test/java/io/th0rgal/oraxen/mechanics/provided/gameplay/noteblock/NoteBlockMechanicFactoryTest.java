package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoteBlockMechanicFactoryTest {

    @Test
    void everyAcceptedVariationUsesItsOwnNonVanillaState() {
        Set<String> states = new HashSet<>();
        for (int variation = 0; variation <= NoteBlockMechanicFactory.MAX_BLOCK_VARIATION; variation++) {
            String state = NoteBlockMechanicFactory.getBlockstateVariantName(variation);
            assertTrue(states.add(state), "Variation " + variation + " reuses " + state);
            assertFalse(state.startsWith("instrument=harp,") && state.endsWith(",powered=false"),
                    "Variation " + variation + " uses the vanilla note block state " + state);
        }
    }
}
