package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.combat.spell.SpellMechanic;
import org.bukkit.configuration.ConfigurationSection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpellMechanicTest extends MechanicTestSupport {

    @Test
    void readsFiniteCharges() {
        TestSpellMechanic mechanic = new TestSpellMechanic(mechanicSection("spell", "charges", 5, "delay", 20L));

        assertEquals(5, mechanic.getMaxCharges());
        assertEquals(2, mechanic.getItemModifiers().length);
    }

    private static class TestSpellMechanic extends SpellMechanic {
        private TestSpellMechanic(ConfigurationSection section) {
            super(mechanicFactory(), section);
        }
    }
}
