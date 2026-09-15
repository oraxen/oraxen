package io.th0rgal.oraxen.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Isolated
class ItemUtilsTest {

    @Test
    void usesLegacyDisplayNameApiBefore1214() {
        ItemMeta meta = mock(ItemMeta.class);
        Component empty = Component.empty();
        when(meta.hasDisplayName()).thenReturn(true);
        when(meta.displayName()).thenReturn(empty);

        try (MockedStatic<VersionUtil> versions = mockStatic(VersionUtil.class)) {
            versions.when(() -> VersionUtil.atOrAbove("1.21.4")).thenReturn(false);

            assertTrue(ItemUtils.hasDisplayName(meta));
            assertSame(empty, ItemUtils.getDisplayName(meta));
            ItemUtils.setDisplayName(meta, null);

            verify(meta).displayName(null);
        }
    }

    @Test
    void usesCustomNameApiFrom1214AndPreservesEmptyComponent() {
        ItemMeta meta = mock(ItemMeta.class);
        Component empty = Component.empty();
        when(meta.hasCustomName()).thenReturn(true);
        when(meta.customName()).thenReturn(empty);

        try (MockedStatic<VersionUtil> versions = mockStatic(VersionUtil.class)) {
            versions.when(() -> VersionUtil.atOrAbove("1.21.4")).thenReturn(true);

            assertTrue(ItemUtils.hasDisplayName(meta));
            assertSame(empty, ItemUtils.getDisplayName(meta));
            ItemUtils.setDisplayName(meta, empty);
            ItemUtils.setDisplayName(meta, null);

            verify(meta).customName(empty);
            verify(meta).customName(null);
        }
    }
}
