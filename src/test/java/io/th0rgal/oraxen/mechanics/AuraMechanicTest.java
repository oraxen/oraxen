package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.AuraMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuraMechanicTest extends MechanicTestSupport {

    @Test
    void createsSimpleAuraFromSettings() {
        AuraMechanic mechanic = new AuraMechanic(mechanicFactory(), mechanicSection("aura", "particle", "FLAME", "type", "simple"));

        assertEquals("test_item", mechanic.getItemID());
    }
}
