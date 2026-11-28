package io.th0rgal.oraxen.pack.generation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

final class PackOutputLocation {
    private static final String DEFAULT_LOCATION = "plugins/Oraxen/pack/pack.zip";

    private PackOutputLocation() {
    }

    static File defaultFile(File sourceFolder) {
        return new File(sourceFolder, sourceFolder.getName() + ".zip");
    }

    static File resolve(File sourceFolder, Object configured) throws IOException {
        if (configured == null) return defaultFile(sourceFolder);
        String location = configured.toString();
        if (location.isBlank() || location.equals(DEFAULT_LOCATION)) return defaultFile(sourceFolder);

        Path target = Path.of(location).toFile().getCanonicalFile().toPath();
        String filename = target.getFileName() == null ? "" : target.getFileName().toString().toLowerCase(Locale.ROOT);
        if (filename.length() <= 4 || !filename.endsWith(".zip") || Files.isDirectory(target)) {
            throw new IOException("Expected a path to a .zip file");
        }
        Path source = sourceFolder.getCanonicalFile().toPath();
        if (target.startsWith(source) && !source.equals(target.getParent())) {
            throw new IOException("The ZIP must be at the source folder root or outside it");
        }
        return target.toFile();
    }

    static String packName(File packFile) {
        String filename = packFile.getName();
        return filename.substring(0, filename.length() - 4);
    }
}
