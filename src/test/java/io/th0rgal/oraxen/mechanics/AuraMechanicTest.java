package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.AuraMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AuraMechanicTest extends MechanicTestSupport {

    @Test
    void createsSimpleAuraFromSettings() {
        AuraMechanic mechanic = new AuraMechanic(mechanicFactory(), mechanicSection("aura", "particle", "FLAME", "type", "simple"));

        assertEquals("test_item", mechanic.getItemID());
    }

    @Test
    void invalidSettingsDisableTheAuraInsteadOfBreakingTheItem() {
        allowLogging();
        assertDoesNotThrow(() -> new AuraMechanic(mechanicFactory(), mechanicSection("aura", "particle", "FLAME", "type", "spiral")));
        assertDoesNotThrow(() -> new AuraMechanic(mechanicFactory(), mechanicSection("aura", "particle", "FLAME")));
        assertDoesNotThrow(() -> new AuraMechanic(mechanicFactory(), mechanicSection("aura", "type", "ring")));
    }
}
