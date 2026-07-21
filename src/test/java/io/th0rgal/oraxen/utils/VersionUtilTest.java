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
    void nmsVersionMatchesSupported26Releases() {
        assertEquals(VersionUtil.NMSVersion.v1_21_R6, VersionUtil.getNMSVersion(new MinecraftVersion("1.21.11")));
        assertEquals(VersionUtil.NMSVersion.v26_1_2, VersionUtil.getNMSVersion(new MinecraftVersion("26.1.2")));
        assertEquals(VersionUtil.NMSVersion.v26_1_2, VersionUtil.getNMSVersion(new MinecraftVersion("26.1.2-alpha")));
        assertEquals(VersionUtil.NMSVersion.v26_1_2, VersionUtil.getNMSVersion(new MinecraftVersion("26.2")));
        assertEquals(VersionUtil.NMSVersion.v26_1_2, VersionUtil.getNMSVersion(new MinecraftVersion("1.26.2")));
    }
}
