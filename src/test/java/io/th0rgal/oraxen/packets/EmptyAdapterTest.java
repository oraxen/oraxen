package io.th0rgal.oraxen.packets;

import io.th0rgal.oraxen.utils.ResourcePackFormatUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmptyAdapterTest {

    @Test
    void latestVersionFollowsTheServerVersion() {
        assertEquals("26.2", PacketAdapter.EmptyAdapter.latestMCVersion("26.2"));
        assertEquals("1.21.8", PacketAdapter.EmptyAdapter.latestMCVersion("1.21.8"));
    }

    @Test
    void preReleaseSuffixesAreStripped() {
        assertEquals("26.3", PacketAdapter.EmptyAdapter.latestMCVersion("26.3-pre1"));
        assertEquals("1.21.9", PacketAdapter.EmptyAdapter.latestMCVersion(" 1.21.9-rc1 "));
    }

    @Test
    void snapshotIdsFallBackToTheNewestKnownVersion() {
        String fallback = ResourcePackFormatUtil.getLatestKnownVersion().getVersion();
        assertEquals(fallback, PacketAdapter.EmptyAdapter.latestMCVersion("25w03a"));
        assertEquals(fallback, PacketAdapter.EmptyAdapter.latestMCVersion(null));
    }
}
