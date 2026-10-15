package io.th0rgal.oraxen.studio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Set;

import io.th0rgal.oraxen.utils.HashUtils;

/**
 * Path and hashing utilities for Oraxen Studio synchronization.
 */
public final class StudioBundleUtils {

    private static final Set<String> TRACKED_DIRECTORIES = Set.of("items", "glyphs", "recipes", "pack");
    private static final Set<String> TRACKED_ROOT_FILES = Set.of(
            "hud.yml", "font.yml", "sounds.yml", "sound.yml", "paintings.yml", "mechanics.yml");

    private StudioBundleUtils() {
    }

    public static String normalizeStudioUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return "https://studio.oraxen.com";
        }
        String trimmed = rawUrl.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "https://" + trimmed;
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    public static boolean isTrackableRelativePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) return false;
        String clean = rawPath.replace('\\', '/');
        while (clean.startsWith("/")) {
            clean = clean.substring(1);
        }
        if (clean.isEmpty() || clean.length() > 512) return false;
        // Rejects "." and ".." segments as well as dot-files (same rule as Studio's normalizeSyncPath).
        if (clean.startsWith(".") || clean.contains("/.") || clean.contains("//") || clean.endsWith("/")) return false;
        // Studio uses paths in storage keys and URLs; it rejects these characters (keep in sync with lib/studio/paths.ts).
        if (hasUnsupportedCharacters(clean)) return false;

        String lower = clean.toLowerCase(Locale.ROOT);
        if (lower.equals("settings.yml") || lower.equals("pack.zip") || lower.endsWith("/pack.zip")) {
            return false;
        }

        // Exact case, like Studio: Oraxen only reads these exact folder and file names.
        int slashIndex = clean.indexOf('/');
        if (slashIndex < 0) {
            return TRACKED_ROOT_FILES.contains(clean);
        }
        return TRACKED_DIRECTORIES.contains(clean.substring(0, slashIndex));
    }

    /** True for characters Studio cannot store in a path: {@code % ? #} and control characters. */
    public static boolean hasUnsupportedCharacters(String path) {
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c < 0x20 || c == 0x7f || c == '%' || c == '?' || c == '#') return true;
        }
        return false;
    }

    public static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HashUtils.bytesToHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public static String sha256Hex(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HashUtils.bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
