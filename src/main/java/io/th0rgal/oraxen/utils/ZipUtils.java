package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.pack.generation.DuplicationHandler;
import io.th0rgal.oraxen.utils.logs.Logs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    private static final Map<String, Integer> COMPRESSION_LEVELS = new LinkedHashMap<>();

    static {
        COMPRESSION_LEVELS.put("NO_COMPRESSION", Deflater.NO_COMPRESSION);
        COMPRESSION_LEVELS.put("BEST_SPEED", Deflater.BEST_SPEED);
        COMPRESSION_LEVELS.put("DEFAULT_COMPRESSION", Deflater.DEFAULT_COMPRESSION);
        COMPRESSION_LEVELS.put("BEST_COMPRESSION", Deflater.BEST_COMPRESSION);
    }

    private ZipUtils() {
    }

    private static int compressionLevel() {
        String configured = String.valueOf(Settings.COMPRESSION.getValue());
        Integer level = COMPRESSION_LEVELS.get(configured.toUpperCase(Locale.ROOT));
        if (level != null) return level;
        Logs.logWarning("Invalid pack.compression value " + configured + ", using BEST_COMPRESSION. Valid values are "
                + String.join(", ", COMPRESSION_LEVELS.keySet()));
        return Deflater.BEST_COMPRESSION;
    }

    public static void writeZipFile(final File outputFile,
            final List<VirtualFile> fileList) {

        final int compressionLevel = compressionLevel();
        try {
            try (final FileOutputStream fos = new FileOutputStream(outputFile);
                final ZipOutputStream zos = new ZipOutputStream(fos, StandardCharsets.UTF_8)) {
                zos.setLevel(compressionLevel);
                zos.setComment(Settings.COMMENT.toString());
                for (final VirtualFile file : fileList) {
                    addToZip(file.getPath(), file.getInputStream(), zos);
                }
            }
            if (Settings.PACK_SIZE_SPOOFING.toBool()) ZipSizeSpoofer.spoof(outputFile.toPath());
        } catch (final IOException ex) {
            Logs.logError("Failed to write resource pack zip");
            Logs.debug(ex);
        }
    }

    public static void addToZip(String zipFilePath, final InputStream fis, ZipOutputStream zos) throws IOException {
        final ZipEntry zipEntry = new ZipEntry(zipFilePath);
        zipEntry.setLastModifiedTime(FileTime.fromMillis(0L));
        if (!DuplicationHandler.tryOpenEntry(zos, zipEntry)) {
            if (fis != null) fis.close();
            return;
        }

        if (fis == null) {
            Logs.logWarning("Skipping zip entry with null input stream: " + zipFilePath);
            try {
                zos.closeEntry();
            } catch (IOException ignored) {
            }
            return;
        }

        try (InputStream in = fis) {
            in.transferTo(zos);
        } catch (IOException exception) {
            Logs.logWarning("Failed to write zip entry " + zipFilePath);
            Logs.debug(exception);
        } finally {
            try {
                zos.closeEntry();
            } catch (IOException ignored) {
            }
        }
    }
}
