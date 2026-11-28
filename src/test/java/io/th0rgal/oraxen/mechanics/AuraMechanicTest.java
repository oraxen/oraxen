package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.AuraMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AuraMechanicTest extends MechanicTestSupport {

    @Test
    void invalidSettingsDisableTheAuraInsteadOfBreakingTheItem() {
        allowLogging();
        assertDoesNotThrow(() -> new AuraMechanic(mechanicFactory(), mechanicSection("aura", "particle", "FLAME", "type", "spiral")));
        assertDoesNotThrow(() -> new AuraMechanic(mechanicFactory(), mechanicSection("aura", "particle", "FLAME")));
        assertDoesNotThrow(() -> new AuraMechanic(mechanicFactory(), mechanicSection("aura", "type", "ring")));
    }
}
