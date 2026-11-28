package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.misc.soulbound.SoulBoundMechanic;
import io.th0rgal.oraxen.mechanics.provided.misc.soulbound.SoulBoundMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.misc.soulbound.SoulBoundMechanicListener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class SoulBoundMechanicTest extends MechanicTestSupport {

    @Test
    void loseChanceIsAPercentage() {
        SoulBoundMechanicFactory factory = mock(SoulBoundMechanicFactory.class);
        SoulBoundMechanic mechanic = new SoulBoundMechanic(factory, mechanicSection("soulbound", "lose_chance", 30));
        when(factory.getMechanic("test_item")).thenReturn(mechanic);
        ItemStack item = mock(ItemStack.class);
        List<ItemStack> drops = new ArrayList<>(Collections.nCopies(1000, item));
        List<ItemStack> kept = new ArrayList<>();
        PlayerDeathEvent event = mock(PlayerDeathEvent.class);
        when(event.getDrops()).thenReturn(drops);
        when(event.getItemsToKeep()).thenReturn(kept);

        try (MockedStatic<OraxenItems> items = mockStatic(OraxenItems.class)) {
            items.when(() -> OraxenItems.getIdByItem(item)).thenReturn("test_item");
            new SoulBoundMechanicListener(factory).onPlayerDeath(event);
        }

        assertEquals(1000, kept.size() + drops.size());
        assertTrue(kept.size() > 600 && kept.size() < 800, "Expected about 700 kept items, got " + kept.size());
    }
}
