package io.th0rgal.oraxen.pack.generation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PackOutputLocation {
    private PackOutputLocation() {
    }

    static File defaultFile(File sourceFolder) {
        return new File(sourceFolder, sourceFolder.getName() + ".zip");
    }

    static File resolve(File sourceFolder, Object configured) throws IOException {
        if (usesDefaultLocation(configured)) {
            return defaultFile(sourceFolder);
        }
        if (!(configured instanceof String location)) throw new IOException("Expected a ZIP file path");

        Path requested = Path.of(location);
        Path target = requested.toFile().getCanonicalFile().toPath();
        Path source = sourceFolder.getCanonicalFile().toPath();
        if (!isZip(requested) || !isZip(target) || Files.isDirectory(target)) {
            throw new IOException("Expected a path to a .zip file");
        }
        // Only root ZIPs are excluded from collection in the pack source folder.
        if (isNested(requested.toAbsolutePath().normalize(), sourceFolder.toPath().toAbsolutePath().normalize())
                || isNested(target, source)) {
            throw new IOException("The ZIP must be at the source folder root or outside it");
        }
        return target.toFile();
    }

    private static boolean usesDefaultLocation(Object configured) {
        return configured == null || (configured instanceof String value
                && (value.isBlank() || value.equals("plugins/Oraxen/pack/pack.zip")));
    }

    static String packName(File mainPack) {
        String filename = mainPack.getName();
        return filename.substring(0, filename.length() - 4);
    }

    /** Client ZIPs and any configured alias of the main ZIP. */
    public static List<File> clientPackFiles(File sourceFolder, Object configured, boolean multiVersion) {
        File mainPack;
        List<File> files = new ArrayList<>();
        try {
            mainPack = resolve(sourceFolder, configured);
            if (!usesDefaultLocation(configured) && configured instanceof String value) {
                addConfiguredAlias(value, mainPack, files);
            }
        } catch (IOException | InvalidPathException exception) {
            mainPack = defaultFile(sourceFolder);
        }
        files.add(mainPack);
        if (multiVersion) files.addAll(PackVersionManager.versionPackFiles(mainPack));
        return files;
    }

    private static void addConfiguredAlias(String value, File mainPack, List<File> files) {
        Path requested = Path.of(value).toAbsolutePath().normalize();
        if (!Files.isSymbolicLink(requested)) return;
        try {
            if (requested.toFile().getCanonicalFile().equals(mainPack)) files.add(requested.toFile());
        } catch (IOException ignored) {
            // The alias is optional; keep excluding the resolved output.
        }
    }

    private static boolean isNested(Path target, Path source) {
        return target.startsWith(source) && !source.equals(target.getParent());
    }

    private static boolean isZip(Path path) {
        return path.getFileName() != null && path.getFileName().toString().length() > 4
                && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip");
    }
}
