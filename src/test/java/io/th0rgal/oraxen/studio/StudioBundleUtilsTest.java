package io.th0rgal.oraxen.studio;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudioBundleUtilsTest {

    @Test
    void normalizeStudioUrl_stripsTrailingSlashAndDefaultsScheme() {
        assertEquals("https://studio.oraxen.com", StudioBundleUtils.normalizeStudioUrl(""));
        assertEquals("https://studio.oraxen.com", StudioBundleUtils.normalizeStudioUrl("studio.oraxen.com/"));
        assertEquals("http://localhost:3000", StudioBundleUtils.normalizeStudioUrl("http://localhost:3000///"));
    }

    @Test
    void isTrackableRelativePath_allowsConfigAndPackAssetsAndRejectsSecrets() {
        assertTrue(StudioBundleUtils.isTrackableRelativePath("items/weapons.yml"));
        assertTrue(StudioBundleUtils.isTrackableRelativePath("glyphs/emoji.yml"));
        assertTrue(StudioBundleUtils.isTrackableRelativePath("recipes/shaped.yml"));
        assertTrue(StudioBundleUtils.isTrackableRelativePath("pack/textures/default/emerald_sword.png"));
        assertTrue(StudioBundleUtils.isTrackableRelativePath("hud.yml"));
        assertTrue(StudioBundleUtils.isTrackableRelativePath("sounds.yml"));

        assertFalse(StudioBundleUtils.isTrackableRelativePath("settings.yml"));
        assertFalse(StudioBundleUtils.isTrackableRelativePath("pack/pack.zip"));
        assertFalse(StudioBundleUtils.isTrackableRelativePath("../secrets.yml"));
        assertFalse(StudioBundleUtils.isTrackableRelativePath(".studio-backup/items/weapons.yml"));
    }

    @Test
    void dispatchIncomingRealtimeFrame_parsesPhoenixJoinAndSyncBroadcast() {
        AtomicBoolean joined = new AtomicBoolean(false);
        AtomicReference<String> receivedEvent = new AtomicReference<>();
        AtomicInteger receivedRev = new AtomicInteger(-1);

        String joinReply = "{\"ref\":\"1\",\"event\":\"phx_reply\",\"payload\":{\"status\":\"ok\",\"response\":{}},\"topic\":\"realtime:oraxen_ws_123\"}";
        StudioWebSocketClient.dispatchIncomingRealtimeFrame(joinReply, () -> joined.set(true), (ev, pl) -> {});
        assertTrue(joined.get());

        String broadcastFrame = "{\"ref\":null,\"event\":\"broadcast\",\"payload\":{\"event\":\"sync\",\"payload\":{\"rev\":7},\"type\":\"broadcast\"},\"topic\":\"realtime:oraxen_ws_123\"}";
        StudioWebSocketClient.dispatchIncomingRealtimeFrame(broadcastFrame, null, (String ev, JsonObject pl) -> {
            receivedEvent.set(ev);
            receivedRev.set(pl.get("rev").getAsInt());
        });
        assertEquals("sync", receivedEvent.get());
        assertEquals(7, receivedRev.get());
    }
}
