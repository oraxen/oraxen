package io.th0rgal.oraxen.studio;

import io.th0rgal.oraxen.studio.StudioApiClient.FileEntry;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * File-level planning and application of Studio pulls, independent of HTTP so it
 * can be unit tested. Only paths accepted by
 * {@link StudioBundleUtils#isTrackableRelativePath} are ever read, written or deleted.
 */
public final class StudioFileSync {

    private StudioFileSync() {
    }

    /** SHA-256 and size of every tracked file in the Oraxen data folder. */
    public static Map<String, FileEntry> localManifest(Path dataFolder) throws IOException {
        return localManifest(dataFolder, new ArrayList<>());
    }

    /**
     * @param unsupported receives files that would be tracked but whose names Studio cannot
     *                    store (see {@link StudioBundleUtils#hasUnsupportedCharacters})
     */
    public static Map<String, FileEntry> localManifest(Path dataFolder, List<String> unsupported) throws IOException {
        Map<String, FileEntry> out = new TreeMap<>();
        if (!Files.isDirectory(dataFolder)) return out;
        List<Path> files;
        try (var stream = Files.walk(dataFolder)) {
            files = stream.filter(Files::isRegularFile).toList();
        }
        for (Path file : files) {
            String rel = dataFolder.relativize(file).toString().replace('\\', '/');
            if (!StudioBundleUtils.isTrackableRelativePath(rel)) {
                if (StudioBundleUtils.hasUnsupportedCharacters(rel)) unsupported.add(rel);
                continue;
            }
            out.put(rel, new FileEntry(StudioBundleUtils.sha256Hex(file), Files.size(file)));
        }
        return out;
    }

    /** Hashes of the trackable paths of a manifest, as recorded for the next sync. */
    public static Map<String, String> hashes(Map<String, FileEntry> manifest) {
        Map<String, String> out = new LinkedHashMap<>();
        manifest.forEach((path, entry) -> {
            if (StudioBundleUtils.isTrackableRelativePath(path)) out.put(path, entry.sha256());
        });
        return out;
    }

    /**
     * @param download     remote files missing or different locally
     * @param delete       local tracked files Studio deleted and this server left unchanged
     * @param localChanges files among the above that were also edited on this server since
     *                     the last sync; their local version is backed up before being replaced
     */
    public record PullPlan(List<String> download, List<String> delete, List<String> localChanges) {
        public boolean isEmpty() {
            return download.isEmpty() && delete.isEmpty();
        }
    }

    public static PullPlan planPull(Map<String, FileEntry> remote, Map<String, FileEntry> local,
                                    @Nullable Map<String, String> base) {
        List<String> download = new ArrayList<>();
        List<String> delete = new ArrayList<>();
        List<String> localChanges = new ArrayList<>();
        for (Map.Entry<String, FileEntry> e : remote.entrySet()) {
            if (!StudioBundleUtils.isTrackableRelativePath(e.getKey())) continue;
            FileEntry mine = local.get(e.getKey());
            if (mine != null && mine.sha256().equalsIgnoreCase(e.getValue().sha256())) continue;
            download.add(e.getKey());
            if (mine != null && changedSinceBase(e.getKey(), mine, base)) localChanges.add(e.getKey());
        }
        for (Map.Entry<String, FileEntry> e : local.entrySet()) {
            if (remote.containsKey(e.getKey())) continue;
            // Only files Studio deleted are removed, and only if this server did not change
            // them since the last sync. Files added here since then are kept (a later push
            // uploads them).
            if (base != null && !changedSinceBase(e.getKey(), e.getValue(), base)) delete.add(e.getKey());
        }
        Collections.sort(download);
        Collections.sort(delete);
        Collections.sort(localChanges);
        return new PullPlan(download, delete, localChanges);
    }

    private static boolean changedSinceBase(String path, FileEntry mine, @Nullable Map<String, String> base) {
        if (base == null) return true; // never synced: anything local is the admin's own work
        String synced = base.get(path);
        return synced == null || !synced.equalsIgnoreCase(mine.sha256());
    }

    /** Downloads one file; implemented over signed URLs in production. */
    @FunctionalInterface
    public interface Downloader {
        void download(String path, Path target) throws IOException, InterruptedException;
    }

    /**
     * Applies a pull: every download is fetched to a temporary file and checked against
     * the expected SHA-256 before anything in the data folder changes. Replaced or deleted
     * files that were edited locally are copied to {@code .studio-backup/} first.
     *
     * @return the paths that changed locally
     */
    public static List<String> applyPull(Path dataFolder, PullPlan plan, Map<String, FileEntry> remote,
                                         Downloader downloader) throws IOException, InterruptedException {
        Path root = dataFolder.toAbsolutePath().normalize();
        // Stage next to the data folder so the final moves are same-filesystem renames.
        Files.createDirectories(root.resolve(".studio"));
        Path staging = Files.createTempDirectory(root.resolve(".studio"), "pull-");
        try {
            Map<String, Path> fetched = new LinkedHashMap<>();
            for (String rel : plan.download()) {
                Path target = staging.resolve(Integer.toString(fetched.size()));
                downloader.download(rel, target);
                String actual = StudioBundleUtils.sha256Hex(target);
                if (!actual.equalsIgnoreCase(remote.get(rel).sha256())) {
                    throw new IOException("Downloaded " + rel + " does not match its Studio checksum");
                }
                fetched.put(rel, target);
            }

            List<String> changed = new ArrayList<>();
            for (Map.Entry<String, Path> e : fetched.entrySet()) {
                Path target = resolveInside(root, e.getKey());
                if (plan.localChanges().contains(e.getKey())) backup(root, e.getKey(), target);
                Files.createDirectories(target.getParent());
                Files.move(e.getValue(), target, StandardCopyOption.REPLACE_EXISTING);
                changed.add(e.getKey());
            }
            for (String rel : plan.delete()) {
                Path target = resolveInside(root, rel);
                if (plan.localChanges().contains(rel)) backup(root, rel, target);
                if (Files.deleteIfExists(target)) changed.add(rel);
            }
            return changed;
        } finally {
            try (var stream = Files.walk(staging)) {
                for (Path p : stream.sorted(Collections.reverseOrder()).toList()) Files.deleteIfExists(p);
            }
        }
    }

    private static Path resolveInside(Path root, String rel) throws IOException {
        if (!StudioBundleUtils.isTrackableRelativePath(rel)) throw new IOException("Refusing untracked path " + rel);
        Path target = root.resolve(rel).normalize();
        if (!target.startsWith(root)) throw new IOException("Refusing path outside the Oraxen folder: " + rel);
        return target;
    }

    private static void backup(Path root, String rel, Path source) throws IOException {
        if (!Files.isRegularFile(source)) return;
        Path backup = root.resolve(".studio-backup").resolve(rel).normalize();
        if (!backup.startsWith(root.resolve(".studio-backup"))) return;
        Files.createDirectories(backup.getParent());
        Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING);
    }
}
