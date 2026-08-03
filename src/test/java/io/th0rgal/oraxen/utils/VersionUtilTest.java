package io.th0rgal.oraxen.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VersionUtilTest {

    @Test
    void mojangVersionNamespaceOrdersAfterLegacy121Versions() {
        assertFalse(new MinecraftVersion("1.21.11").isAtLeast(new MinecraftVersion("26.1.2")));
        assertTrue(new MinecraftVersion("26.1.2").isAtLeast(new MinecraftVersion("1.21.11")));
    }

    @Test
    void modernVersionNamespaceMatches26Releases() {
        assertFalse(VersionUtil.isModernVersionNamespace(new MinecraftVersion("1.21.11")));
        assertTrue(VersionUtil.isModernVersionNamespace(new MinecraftVersion("26.1.2")));
        assertTrue(VersionUtil.isModernVersionNamespace(new MinecraftVersion("26.1.2-alpha")));
        assertTrue(VersionUtil.isModernVersionNamespace(new MinecraftVersion("26.2")));
        assertTrue(VersionUtil.isModernVersionNamespace(new MinecraftVersion("1.26.2")));
        assertTrue(VersionUtil.isModernVersionNamespace(new MinecraftVersion("26.3")));
    }
}
