package io.th0rgal.oraxen.pack.generation;

import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.logs.Logs;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Writes an additional ordinary ZIP without changing the files used for client packs. */
final class UnprotectedPackWriter {

    private UnprotectedPackWriter() {
    }

    static void writeConfigured(List<VirtualFile> output, File packFolder) {
        Object location = Settings.UNPROTECTED_PACK_LOCATION.getValue();
        if (location == null || location.toString().isBlank()) return;

        try {
            write(Path.of(location.toString()), output, packFolder);
        } catch (IOException | RuntimeException exception) {
            Logs.logWarning("Failed to write unprotected resource pack: " + exception.getMessage());
        }
    }

    static void write(Path destination, List<VirtualFile> output, File packFolder) throws IOException {
        Path target = destination.toFile().getCanonicalFile().toPath();
        Path sourceFolder = packFolder.getCanonicalFile().toPath();
        // Avoid overwriting client packs or importing the exported ZIP on the next generation.
        if (target.startsWith(sourceFolder)) {
            throw new IOException("unprotected-location must be outside the resource-pack source folder: " + sourceFolder);
        }

        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), ".oraxen-unprotected-", ".zip");
        try {
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(temporary))) {
                Set<String> paths = new HashSet<>();
                for (VirtualFile file : output) {
                    if (!paths.add(file.getPath())) continue;
                    byte[] content;
                    try (InputStream input = file.getInputStream()) {
                        if (input == null) throw new IOException("Cannot read " + file.getPath());
                        content = input.readAllBytes();
                    }
                    // Event listeners may supply one-shot streams. Restore before any ZIP writes
                    // so an export failure cannot leave the client pack with consumed streams.
                    file.setInputStream(new ByteArrayInputStream(content));
                    ZipEntry entry = new ZipEntry(file.getPath());
                    entry.setLastModifiedTime(FileTime.fromMillis(0L));
                    zip.putNextEntry(entry);
                    zip.write(content);
                    zip.closeEntry();
                }
            }
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
