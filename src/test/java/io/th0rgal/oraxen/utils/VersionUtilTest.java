package io.th0rgal.oraxen.utils;

import org.junit.jupiter.api.Test;
import java.util.jar.Attributes;

import static org.junit.jupiter.api.Assertions.*;

class VersionUtilTest {

    @Test
    void missingManifestKeysDoNotBorrowUnrelatedValues() {
        Attributes attributes = new Attributes();
        attributes.putValue("Unrelated", "true");
        assertFalse(VersionUtil.isCompiled(attributes));
        assertFalse(VersionUtil.isValidCompiler(attributes));
        attributes.putValue("Compiled", "true");
        assertTrue(VersionUtil.isCompiled(attributes));
    }

    @Test
    void compilerRulesPreserveTrustedNamesAndBooleanSemantics() {
        Attributes attributes = new Attributes();
        attributes.putValue("Compiled", " TRUE ");
        for (String compiler : new String[]{" SIVERT ", "Thomas"}) {
            attributes.putValue("Built-By", compiler);
            assertTrue(VersionUtil.isValidCompiler(attributes));
            assertFalse(VersionUtil.isCompiled(attributes));
        }
        attributes.putValue("Built-By", "other");
        assertTrue(VersionUtil.isCompiled(attributes));
        attributes.putValue("Compiled", "invalid");
        assertFalse(VersionUtil.isCompiled(attributes));
    }

    @Test
    void snapshotParsingDoesNotRequireAnInitializedPacketAdapter() {
        assertDoesNotThrow(() -> new MinecraftVersion("25w03a"));
    }

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
    void packetAccessorCheckIncludes263PreReleases() {
        assertFalse(VersionUtil.uses263PacketAccessors(new MinecraftVersion("26.2")));
        assertFalse(VersionUtil.uses263PacketAccessors(new MinecraftVersion("1.26.2")));
        assertTrue(VersionUtil.uses263PacketAccessors(new MinecraftVersion("26.3")));
        assertTrue(VersionUtil.uses263PacketAccessors(new MinecraftVersion("26.3-pre1")));
        assertTrue(VersionUtil.uses263PacketAccessors(new MinecraftVersion("26.3-rc1")));
        assertTrue(VersionUtil.uses263PacketAccessors(new MinecraftVersion("1.26.3")));
        assertTrue(VersionUtil.uses263PacketAccessors(new MinecraftVersion(26, 3, 0, "pre1")));
        assertFalse(new MinecraftVersion(26, 3, 0, "pre1").isAtLeast(new MinecraftVersion("26.3")));
    }

    @Test
    void nmsHandlerRequires1212OrLater() {
        assertFalse(VersionUtil.supportsNmsHandler(new MinecraftVersion("1.20.1")));
        assertFalse(VersionUtil.supportsNmsHandler(new MinecraftVersion("1.21.1")));
        assertTrue(VersionUtil.supportsNmsHandler(new MinecraftVersion("1.21.2")));
        assertTrue(VersionUtil.supportsNmsHandler(new MinecraftVersion("26.1.2")));
        assertEquals("Paper and Paper forks 1.21.2+ / 26.x through the guarded NMS handler", VersionUtil.supportedVersions());
    }
}
