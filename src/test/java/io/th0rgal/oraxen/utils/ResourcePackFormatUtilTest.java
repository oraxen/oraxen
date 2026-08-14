package io.th0rgal.oraxen.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourcePackFormatUtilTest {

    @Test
    void modernNamespaceVersionsResolveBeforeLegacyAliases() {
        // 26.1.x must match the "26.1" threshold (84), not the "1.26.2" alias (88),
        // which every major-26 version compares greater than.
        assertEquals(84, ResourcePackFormatUtil.getPackFormatForVersion(new MinecraftVersion("26.1")));
        assertEquals(84, ResourcePackFormatUtil.getPackFormatForVersion(new MinecraftVersion("26.1.2")));
        assertEquals(84, ResourcePackFormatUtil.getPackFormatForVersion(new MinecraftVersion("26")));
        assertEquals(88, ResourcePackFormatUtil.getPackFormatForVersion(new MinecraftVersion("26.2")));
    }

    @Test
    void legacyNamespaceAliasesStillResolve() {
        assertEquals(84, ResourcePackFormatUtil.getPackFormatForVersion(new MinecraftVersion("1.26.1")));
        assertEquals(88, ResourcePackFormatUtil.getPackFormatForVersion(new MinecraftVersion("1.26.2")));
        assertEquals(75, ResourcePackFormatUtil.getPackFormatForVersion(new MinecraftVersion("1.21.11")));
    }

    @Test
    void dataPackFormatsResolveForBothNamespaces() {
        assertEquals(101, ResourcePackFormatUtil.getDataPackFormatForVersion(new MinecraftVersion("26")));
        assertEquals(101, ResourcePackFormatUtil.getDataPackFormatForVersion(new MinecraftVersion("26.1")));
        assertEquals(107, ResourcePackFormatUtil.getDataPackFormatForVersion(new MinecraftVersion("26.2")));
        assertEquals(101, ResourcePackFormatUtil.getDataPackFormatForVersion(new MinecraftVersion("1.26.1")));
        assertEquals(107, ResourcePackFormatUtil.getDataPackFormatForVersion(new MinecraftVersion("1.26.2")));
        assertEquals(94, ResourcePackFormatUtil.getDataPackFormatForVersion(new MinecraftVersion("1.21.11")));
        assertEquals(88, ResourcePackFormatUtil.getDataPackFormatForVersion(new MinecraftVersion("1.21.10")));
        assertEquals(88, ResourcePackFormatUtil.getDataPackFormatForVersion(new MinecraftVersion("1.21.9")));
    }
}
