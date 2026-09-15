package io.th0rgal.oraxen.api;

import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OraxenItemsTest {

    @Test
    void itemIdLookupReadsItemMetaOnlyOnce() throws Exception {
        OraxenPlugin plugin = mock(OraxenPlugin.class);
        when(plugin.namespace()).thenReturn("oraxen");

        Field instance = OraxenPlugin.class.getDeclaredField("oraxen");
        instance.setAccessible(true);
        Object previousPlugin = instance.get(null);
        instance.set(null, plugin);
        try {
            ItemStack item = mock(ItemStack.class);
            ItemMeta meta = mock(ItemMeta.class);
            PersistentDataContainer container = mock(PersistentDataContainer.class);
            when(item.getItemMeta()).thenReturn(meta);
            when(meta.getPersistentDataContainer()).thenReturn(container);
            when(container.isEmpty()).thenReturn(true);

            assertNull(OraxenItems.getIdByItem(item));
            verify(item).getItemMeta();
        } finally {
            instance.set(null, previousPlugin);
        }
    }

    @Test
    void accessorsReturnEmptyResultsBeforeItemsHaveLoaded() throws Exception {
        OraxenPlugin plugin = mock(OraxenPlugin.class);
        when(plugin.namespace()).thenReturn("oraxen");

        Field instance = OraxenPlugin.class.getDeclaredField("oraxen");
        instance.setAccessible(true);
        Object previousPlugin = instance.get(null);
        instance.set(null, plugin);
        try {
            assertTrue(OraxenItems.getMap().isEmpty());
            assertFalse(OraxenItems.exists("missing"));
            assertFalse(OraxenItems.exists((ItemStack) null));
            assertTrue(OraxenItems.getUnexcludedItems(new File("missing.yml")).isEmpty());
            assertTrue(OraxenItems.entryStream().findAny().isEmpty());
            assertArrayEquals(new String[0], OraxenItems.getItemNames());
        } finally {
            instance.set(null, previousPlugin);
        }
    }
}
