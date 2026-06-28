package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.combat.knockbackstrike.KnockbackStrikeMechanic;
import org.bukkit.Particle;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnockbackStrikeMechanicTest extends MechanicTestSupport {

    @Test
    void readsSettingsAndTracksRequiredHits() {
        KnockbackStrikeMechanic mechanic = new KnockbackStrikeMechanic(mechanicFactory(), mechanicSection("knockbackstrike",
                "required_hits", 2,
                "knockback_horizontal", 3.0,
                "knockback_vertical", 0.75,
                "reset_time", 100,
                "show_counter", false,
                "play_sound", false,
                "particle", java.util.Map.of("type", "FLAME", "count", 4, "spread", 0.2)));

        assertEquals(2, mechanic.getRequiredHits());
        assertEquals(3.0, mechanic.getKnockbackHorizontal());
        assertEquals(0.75, mechanic.getKnockbackVertical());
        assertEquals(Particle.FLAME, mechanic.getParticleType());
        assertEquals(4, mechanic.getParticleCount());
        assertEquals(0.2, mechanic.getParticleSpread());
        assertFalse(mechanic.shouldPlaySound());
        assertFalse(mechanic.shouldShowCounter());

        UUID player = UUID.randomUUID();
        assertFalse(mechanic.incrementHitAndCheck(player));
        assertEquals(1, mechanic.getCurrentHitCount(player));
        assertTrue(mechanic.incrementHitAndCheck(player));
        assertEquals(0, mechanic.getCurrentHitCount(player));
    }
}
