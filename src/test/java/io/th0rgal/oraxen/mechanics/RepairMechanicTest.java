package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.gameplay.repair.RepairMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("removal")
class RepairMechanicTest extends MechanicTestSupport {

    @Test
    void fixedAmountRepairsWithoutGoingBelowZero() {
        RepairMechanic mechanic = new RepairMechanic(mechanicFactory(), mechanicSection("repair", "fixed_amount", 10));

        assertEquals(5, mechanic.getFinalDamage(100, 15));
        assertEquals(0, mechanic.getFinalDamage(100, 5));
    }
}
