package io.th0rgal.oraxen.studio;

import io.th0rgal.oraxen.studio.StudioApiClient.FileEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudioFileSyncTest {

    private static FileEntry entry(String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return new FileEntry(StudioBundleUtils.sha256Hex(bytes), bytes.length);
    }

    private static void write(Path root, String rel, String content) throws IOException {
        Path p = root.resolve(rel);
        Files.createDirectories(p.getParent());
        Files.writeString(p, content, StandardCharsets.UTF_8);
    }

    @Test
    void localManifest_includesOnlyTrackedFilesAndReportsUnsupportedNames(@TempDir Path root) throws IOException {
        write(root, "items/a.yml", "a");
        write(root, "pack/textures/a.png", "png");
        write(root, "settings.yml", "Studio:\n  secret: oxn_studio_x\n");
        write(root, ".studio/state.json", "{}");
        write(root, "items/50%_off.yml", "x");

        List<String> unsupported = new ArrayList<>();
        Map<String, FileEntry> manifest = StudioFileSync.localManifest(root, unsupported);

        assertEquals(List.of("items/a.yml", "pack/textures/a.png"), new ArrayList<>(manifest.keySet()));
        assertEquals(entry("a"), manifest.get("items/a.yml"));
        assertEquals(List.of("items/50%_off.yml"), unsupported);
    }

    @Test
    void planPull_downloadsDifferencesDeletesRemovedFilesAndFlagsLocalEdits() {
        Map<String, FileEntry> remote = Map.of(
                "items/same.yml", entry("same"),
                "items/changed.yml", entry("studio v2"),
                "items/new.yml", entry("new"),
                "items/edited_here.yml", entry("studio v2"));
        Map<String, FileEntry> local = Map.of(
                "items/same.yml", entry("same"),
                "items/changed.yml", entry("v1"),
                "items/edited_here.yml", entry("server edit"),
                "items/removed.yml", entry("old"));
        Map<String, String> base = Map.of(
                "items/same.yml", entry("same").sha256(),
                "items/changed.yml", entry("v1").sha256(),
                "items/edited_here.yml", entry("v1").sha256(),
                "items/removed.yml", entry("old").sha256());

        StudioFileSync.PullPlan plan = StudioFileSync.planPull(remote, local, base);

        assertEquals(List.of("items/changed.yml", "items/edited_here.yml", "items/new.yml"), plan.download());
        assertEquals(List.of("items/removed.yml"), plan.delete());
        assertEquals(List.of("items/edited_here.yml"), plan.localChanges());
    }

    @Test
    void planPull_keepsFilesAddedOrEditedOnTheServerSinceTheLastSync() {
        Map<String, FileEntry> local = Map.of(
                "items/added_here.yml", entry("new"),
                "items/deleted_in_studio_but_edited_here.yml", entry("edited"));
        Map<String, String> base = Map.of("items/deleted_in_studio_but_edited_here.yml", entry("v1").sha256());
        StudioFileSync.PullPlan plan = StudioFileSync.planPull(Map.of(), local, base);
        assertTrue(plan.delete().isEmpty());
    }

    @Test
    void planPull_withoutBaseNeverDeletes() {
        StudioFileSync.PullPlan plan = StudioFileSync.planPull(Map.of(), Map.of("items/mine.yml", entry("x")), null);
        assertTrue(plan.isEmpty());
    }

    @Test
    void hashes_recordsOnlyTrackablePaths() {
        Map<String, String> h = StudioFileSync.hashes(Map.of("items/a.yml", entry("a"), "settings.yml", entry("s")));
        assertEquals(java.util.Set.of("items/a.yml"), h.keySet());
        assertTrue(StudioBundleUtils.isTrackableRelativePath("items/sword..yml"));
        assertFalse(StudioBundleUtils.isTrackableRelativePath("items/../settings.yml"));
    }

    @Test
    void planPull_withoutBaseTreatsEveryLocalDifferenceAsALocalEdit() {
        StudioFileSync.PullPlan plan = StudioFileSync.planPull(
                Map.of("items/a.yml", entry("studio")), Map.of("items/a.yml", entry("server")), null);
        assertEquals(List.of("items/a.yml"), plan.localChanges());
    }

    @Test
    void planPull_ignoresUntrackedRemotePaths() {
        StudioFileSync.PullPlan plan = StudioFileSync.planPull(
                Map.of("settings.yml", entry("x"), "../escape.yml", entry("x")), Map.of(), Map.of());
        assertTrue(plan.isEmpty());
    }

    @Test
    void applyPull_writesVerifiedFilesBacksUpLocalEditsAndDeletes(@TempDir Path root) throws Exception {
        write(root, "items/edited_here.yml", "server edit");
        write(root, "items/removed.yml", "old");
        Map<String, FileEntry> remote = Map.of(
                "items/edited_here.yml", entry("studio"),
                "pack/textures/new.png", entry("png"));
        StudioFileSync.PullPlan plan = new StudioFileSync.PullPlan(
                List.of("items/edited_here.yml", "pack/textures/new.png"),
                List.of("items/removed.yml"),
                List.of("items/edited_here.yml"));
        Map<String, String> served = Map.of("items/edited_here.yml", "studio", "pack/textures/new.png", "png");

        List<String> changed = StudioFileSync.applyPull(root, plan, remote,
                (path, target) -> Files.writeString(target, served.get(path), StandardCharsets.UTF_8));

        assertEquals(3, changed.size());
        assertEquals("studio", Files.readString(root.resolve("items/edited_here.yml")));
        assertEquals("server edit", Files.readString(root.resolve(".studio-backup/items/edited_here.yml")));
        assertEquals("png", Files.readString(root.resolve("pack/textures/new.png")));
        assertFalse(Files.exists(root.resolve("items/removed.yml")));
    }

    @Test
    void applyPull_rejectsCorruptDownloadsBeforeTouchingAnything(@TempDir Path root) throws Exception {
        write(root, "items/a.yml", "original");
        Map<String, FileEntry> remote = Map.of("items/a.yml", entry("expected"), "items/b.yml", entry("b"));
        StudioFileSync.PullPlan plan = new StudioFileSync.PullPlan(
                List.of("items/b.yml", "items/a.yml"), List.of(), List.of());

        assertThrows(IOException.class, () -> StudioFileSync.applyPull(root, plan, remote,
                (path, target) -> Files.writeString(target, path.equals("items/a.yml") ? "tampered" : "b")));

        assertEquals("original", Files.readString(root.resolve("items/a.yml")));
        assertFalse(Files.exists(root.resolve("items/b.yml")), "nothing is applied when any download is invalid");
    }

    @Test
    void applyPull_refusesPathsOutsideTheDataFolder(@TempDir Path root) {
        Map<String, FileEntry> remote = Map.of("../escape.yml", entry("x"));
        StudioFileSync.PullPlan plan = new StudioFileSync.PullPlan(List.of("../escape.yml"), List.of(), List.of());
        assertThrows(IOException.class, () -> StudioFileSync.applyPull(root, plan, remote,
                (path, target) -> Files.writeString(target, "x")));
        assertFalse(Files.exists(root.getParent().resolve("escape.yml")));
    }

    @Test
    void syncState_roundTripsAndIgnoresStateOfAnotherWorkspace(@TempDir Path root) throws IOException {
        StudioSyncState state = StudioSyncState.load(root, "ws-1");
        assertEquals(0, state.rev());
        assertEquals(null, state.base());

        state.recordSync("ws-1", 7, Map.of("items/a.yml", entry("a").sha256()));
        state.save(root);

        StudioSyncState loaded = StudioSyncState.load(root, "ws-1");
        assertEquals(7, loaded.rev());
        assertEquals(entry("a").sha256(), loaded.base().get("items/a.yml"));
        assertEquals(null, StudioSyncState.load(root, "ws-2").base());
        assertFalse(StudioBundleUtils.isTrackableRelativePath(".studio/state.json"));
    }
}
