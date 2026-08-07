package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.combat.spear.SpearLungeMechanic;
import org.bukkit.Particle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpearLungeMechanicTest extends MechanicTestSupport {

    @Test
    void readsChargeDamageAndIntermediateModelSettings() {
        SpearLungeMechanic mechanic = new SpearLungeMechanic(mechanicFactory(), mechanicSection("spear_lunge",
                "active_model", "default/spear_active",
                "charge_ticks", 8,
                "lunge_velocity", 0.9,
                "max_range", 5.0,
                "damage", 7.0,
                "min_damage", 2.0,
                "knockback", 1.0,
                "hitbox_radius", 0.8,
                "smooth_frames", 2,
                "intermediate_models", List.of("frame0", "frame1"),
                "max_targets", 3,
                "min_charge_percent", 0.5,
                "charge_slowdown", 0.25,
                "max_hold_ticks", 40,
                "particles", java.util.Map.of("enabled", true, "charge", "FLAME", "lunge", "CRIT", "hit", "DAMAGE_INDICATOR")));

        assertTrue(mechanic.hasActiveModel());
        assertEquals("default/spear_active", mechanic.getActiveModelPath());
        assertEquals("oraxen:test_item_active", mechanic.getActiveItemModelKey().toString());
        assertEquals(8, mechanic.getChargeTicks());
        assertEquals(0.9, mechanic.getLungeVelocity());
        assertEquals(5.0, mechanic.getMaxRange());
        assertEquals(7.0, mechanic.getDamage());
        assertEquals(2.0, mechanic.getMinDamage());
        assertEquals(1.0, mechanic.getKnockback());
        assertEquals(0.8, mechanic.getHitboxRadius());
        assertEquals(3, mechanic.getMaxTargets());
        assertEquals(0.5, mechanic.getMinChargePercent());
        assertEquals(0.25, mechanic.getChargeSlowdown());
        assertEquals(40, mechanic.getMaxHoldTicks());
        assertEquals(2, mechanic.getIntermediateModelCount());
        assertEquals("frame1", mechanic.getIntermediateModelPath(1));
        assertEquals("oraxen:test_item_frame1", mechanic.getIntermediateModelKey(1).toString());
        assertTrue(mechanic.hasParticles());
        assertEquals(Particle.FLAME, mechanic.getChargeParticle());
    }
}
