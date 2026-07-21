package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.combat.spell.energyblast.EnergyBlastMechanic;
import org.bukkit.Particle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EnergyBlastMechanicTest extends MechanicTestSupport {

    @Test
    void readsParticleDamageAndLength() {
        EnergyBlastMechanic mechanic = new EnergyBlastMechanic(mechanicFactory(), mechanicSection("energyblast",
                "particle", java.util.Map.of("type", "FLAME"),
                "damage", 4.5,
                "length", 12));

        assertEquals(Particle.FLAME, mechanic.getParticle());
        assertNull(mechanic.getParticleColor());
        assertEquals(4.5, mechanic.getDamage());
        assertEquals(12, mechanic.getLength());
    }
}
