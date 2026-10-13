package io.th0rgal.oraxen.studio;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import io.th0rgal.oraxen.utils.HashUtils;

/**
 * Pure file/zip utilities for Oraxen Studio synchronization.
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
        if (clean.isEmpty() || clean.contains("..")) return false;
        if (clean.startsWith(".") || clean.contains("/.")) return false;

        String lower = clean.toLowerCase(Locale.ROOT);
        if (lower.equals("settings.yml") || lower.equals("pack.zip") || lower.endsWith("/pack.zip")) {
            return false;
        }

        int slashIndex = clean.indexOf('/');
        if (slashIndex < 0) {
            return TRACKED_ROOT_FILES.contains(lower);
        }
        String topDir = clean.substring(0, slashIndex).toLowerCase(Locale.ROOT);
        return TRACKED_DIRECTORIES.contains(topDir);
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

    public static File createWorkspaceZipBundle(Path dataFolder) throws IOException {
        File tempZip = Files.createTempFile("oraxen-studio-push-", ".zip").toFile();
        List<Path> filesToPack = new ArrayList<>();

        if (Files.isDirectory(dataFolder)) {
            try (var stream = Files.walk(dataFolder)) {
                stream.filter(Files::isRegularFile).forEach(filesToPack::add);
            }
        }
        Collections.sort(filesToPack);

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(tempZip.toPath()))) {
            byte[] buffer = new byte[8192];
            for (Path file : filesToPack) {
                String rel = dataFolder.relativize(file).toString().replace('\\', '/');
                if (!isTrackableRelativePath(rel)) continue;

                ZipEntry entry = new ZipEntry(rel);
                zos.putNextEntry(entry);
                try (FileInputStream fis = new FileInputStream(file.toFile())) {
                    int read;
                    while ((read = fis.read(buffer)) != -1) {
                        zos.write(buffer, 0, read);
                    }
                }
                zos.closeEntry();
            }
        }
        return tempZip;
    }

    public static Map<String, String> extractBundleToDataFolder(Path zipFile, Path dataFolder) throws IOException {
        Map<String, String> updatedFiles = new LinkedHashMap<>();
        Path normalizedRoot = dataFolder.toAbsolutePath().normalize();
        Path backupRoot = normalizedRoot.resolve(".studio-backup");

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }
                String rel = entry.getName().replace('\\', '/');
                while (rel.startsWith("/")) {
                    rel = rel.substring(1);
                }
                if (!isTrackableRelativePath(rel)) {
                    zis.closeEntry();
                    continue;
                }

                Path targetPath = normalizedRoot.resolve(rel).normalize();
                if (!targetPath.startsWith(normalizedRoot)) {
                    zis.closeEntry();
                    throw new IOException("Refusing to extract zip entry outside Oraxen data folder: " + rel);
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = zis.read(buffer)) != -1) {
                    baos.write(buffer, 0, read);
                }
                byte[] newBytes = baos.toByteArray();
                String newHash = sha256Hex(newBytes);

                if (Files.isRegularFile(targetPath)) {
                    String existingHash = sha256Hex(targetPath);
                    if (existingHash.equalsIgnoreCase(newHash)) {
                        zis.closeEntry();
                        continue;
                    }
                    Path backupTarget = backupRoot.resolve(rel).normalize();
                    if (backupTarget.startsWith(backupRoot)) {
                        Files.createDirectories(backupTarget.getParent());
                        Files.copy(targetPath, backupTarget, StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                Files.createDirectories(targetPath.getParent());
                Path tmpTarget = targetPath.resolveSibling(targetPath.getFileName().toString() + ".studio.tmp");
                Files.write(tmpTarget, newBytes);
                Files.move(tmpTarget, targetPath, StandardCopyOption.REPLACE_EXISTING);
                updatedFiles.put(rel, newHash);
                zis.closeEntry();
            }
        }
        return updatedFiles;
    }
}
