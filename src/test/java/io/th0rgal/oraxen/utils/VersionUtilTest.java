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

    @Test
    void nmsHandlerRequires1212OrLater() {
        assertFalse(VersionUtil.supportsNmsHandler(new MinecraftVersion("1.20.1")));
        assertFalse(VersionUtil.supportsNmsHandler(new MinecraftVersion("1.21.1")));
        assertTrue(VersionUtil.supportsNmsHandler(new MinecraftVersion("1.21.2")));
        assertTrue(VersionUtil.supportsNmsHandler(new MinecraftVersion("26.1.2")));
        assertEquals("Paper and Paper forks 1.21.2+ / 26.x through the guarded NMS handler", VersionUtil.supportedVersions());
    }

    @Test
    void scoreboardBackgroundHidingIsLimitedToCompatibleVersions() {
        assertFalse(VersionUtil.supportsScoreboardBackgroundHiding(new MinecraftVersion("1.21.5")));
        assertTrue(VersionUtil.supportsScoreboardBackgroundHiding(new MinecraftVersion("1.21.6")));
        assertTrue(VersionUtil.supportsScoreboardBackgroundHiding(new MinecraftVersion("1.21.7")));
        assertFalse(VersionUtil.supportsScoreboardBackgroundHiding(new MinecraftVersion("1.21.8")));
        assertFalse(VersionUtil.supportsScoreboardBackgroundHiding(new MinecraftVersion("26.1")));
    }
}
