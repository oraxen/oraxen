package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.combat.spell.SpellMechanic;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpellMechanicTest extends MechanicTestSupport {

    @Test
    void readsFiniteCharges() {
        TestSpellMechanic mechanic = new TestSpellMechanic(mechanicSection("spell", "charges", 5, "delay", 20L));

        assertEquals(5, mechanic.getMaxCharges());
        assertEquals(2, mechanic.getItemModifiers().length);
    }

    @Test
    void updatesTheChargesLoreLineWhereverItIs() {
        ItemMeta meta = metaWithCharges(3, List.of(Component.text("Forged in fire"), Component.text("Charges 3/5")));

        new TestSpellMechanic(mechanicSection("spell", "charges", 5)).removeCharge(item(meta));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Component>> lore = ArgumentCaptor.forClass(List.class);
        verify(meta).lore(lore.capture());
        assertEquals(List.of(Component.text("Forged in fire"), Component.text("Charges 2/5")), lore.getValue());
    }

    @Test
    void leavesLoreWithoutChargesLineAlone() {
        ItemMeta meta = metaWithCharges(3, new ArrayList<>());

        new TestSpellMechanic(mechanicSection("spell", "charges", 5)).removeCharge(item(meta));

        verify(meta, never()).lore(any());
    }

    private static ItemMeta metaWithCharges(int charges, List<Component> lore) {
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(pdc.has(SpellMechanic.NAMESPACED_KEY, PersistentDataType.INTEGER)).thenReturn(true);
        when(pdc.getOrDefault(SpellMechanic.NAMESPACED_KEY, PersistentDataType.INTEGER, -1)).thenReturn(charges);

        ItemMeta meta = mock(ItemMeta.class);
        when(meta.getPersistentDataContainer()).thenReturn(pdc);
        when(meta.lore()).thenReturn(lore);
        return meta;
    }

    private static ItemStack item(ItemMeta meta) {
        ItemStack item = mock(ItemStack.class);
        when(item.getItemMeta()).thenReturn(meta);
        return item;
    }

    private static class TestSpellMechanic extends SpellMechanic {
        private TestSpellMechanic(ConfigurationSection section) {
            super(mechanicFactory(), section);
        }
    }
}
