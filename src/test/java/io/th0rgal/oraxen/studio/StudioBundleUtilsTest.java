package io.th0rgal.oraxen.studio;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
    void createAndExtractBundle_roundTripsTrackableFilesAndBacksUpModifiedFiles(@TempDir Path sourceDir, @TempDir Path targetDir) throws IOException {
        Files.createDirectories(sourceDir.resolve("items"));
        Files.createDirectories(sourceDir.resolve("pack/textures/default"));
        Files.writeString(sourceDir.resolve("items/weapons.yml"), "emerald_sword:\n  material: DIAMOND_SWORD\n", StandardCharsets.UTF_8);
        Files.writeString(sourceDir.resolve("pack/textures/default/sword.png"), "PNG_V2", StandardCharsets.UTF_8);
        Files.writeString(sourceDir.resolve("settings.yml"), "Studio:\n  secret: oxn_studio_secret\n", StandardCharsets.UTF_8);
        Files.writeString(sourceDir.resolve("pack/pack.zip"), "GENERATED_PACK", StandardCharsets.UTF_8);

        // Pre-populate target with an older sword texture to verify backup creation
        Files.createDirectories(targetDir.resolve("pack/textures/default"));
        Files.writeString(targetDir.resolve("pack/textures/default/sword.png"), "PNG_V1", StandardCharsets.UTF_8);

        File bundle = StudioBundleUtils.createWorkspaceZipBundle(sourceDir);
        try {
            Map<String, String> changed = StudioBundleUtils.extractBundleToDataFolder(bundle.toPath(), targetDir);
            assertEquals(2, changed.size());
            assertTrue(changed.containsKey("items/weapons.yml"));
            assertTrue(changed.containsKey("pack/textures/default/sword.png"));
            assertFalse(Files.exists(targetDir.resolve("settings.yml")));
            assertFalse(Files.exists(targetDir.resolve("pack/pack.zip")));
            assertEquals("PNG_V2", Files.readString(targetDir.resolve("pack/textures/default/sword.png"), StandardCharsets.UTF_8));
            assertEquals("PNG_V1", Files.readString(targetDir.resolve(".studio-backup/pack/textures/default/sword.png"), StandardCharsets.UTF_8));

            // Second extraction with identical bundle should detect 0 changed files
            Map<String, String> secondChanged = StudioBundleUtils.extractBundleToDataFolder(bundle.toPath(), targetDir);
            assertTrue(secondChanged.isEmpty());
        } finally {
            Files.deleteIfExists(bundle.toPath());
        }
    }

    @Test
    void extractBundle_ignoresPathTraversalEntries(@TempDir Path targetDir) throws IOException {
        Path maliciousZip = targetDir.resolve("malicious.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(maliciousZip))) {
            zos.putNextEntry(new ZipEntry("../escaped.yml"));
            zos.write("bad".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("items/valid.yml"));
            zos.write("ok: true".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        Map<String, String> changed = StudioBundleUtils.extractBundleToDataFolder(maliciousZip, targetDir);
        assertEquals(1, changed.size());
        assertTrue(Files.exists(targetDir.resolve("items/valid.yml")));
        assertFalse(Files.exists(targetDir.resolve("../escaped.yml")));
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
