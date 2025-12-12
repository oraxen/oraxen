package io.th0rgal.oraxen.pack.generation;

import io.th0rgal.oraxen.utils.VirtualFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PackMerger functionality.
 * 
 * Run with: ./gradlew :core:test --tests
 * "io.th0rgal.oraxen.pack.generation.PackMergerTest"
 */
public class PackMergerTest {

    @TempDir
    Path tempDir;

    private Path packFolder;
    private Path uploadsFolder;

    @BeforeEach
    void setUp() throws IOException {
        packFolder = tempDir.resolve("pack");
        uploadsFolder = packFolder.resolve("uploads");
        Files.createDirectories(uploadsFolder);
    }

    @Test
    void testStandardPackMerge() throws IOException {
        // Create a standard resource pack zip (assets/ at root)
        createStandardPackZip(uploadsFolder.resolve("standard_pack.zip"));

        TestablePackMerger merger = new TestablePackMerger(packFolder.toFile());
        List<VirtualFile> result = merger.mergeUploadedPacks();

        // Should find assets at proper paths
        List<String> paths = result.stream().map(VirtualFile::getPath).toList();
        System.out.println("Standard pack paths: " + paths);

        assertTrue(paths.stream().anyMatch(p -> p.startsWith("assets/minecraft/")),
                "Standard pack should have assets/minecraft paths, got: " + paths);
    }

    @Test
    void testNestedPackMerge_BattlePassStyle() throws IOException {
        // Create a BattlePass-style nested pack with deeply nested assets
        createNestedPackZip(uploadsFolder.resolve("battlepass.zip"),
                "Textures (use 1)/jars_battlepass(vanillapack)");

        TestablePackMerger merger = new TestablePackMerger(packFolder.toFile());
        List<VirtualFile> result = merger.mergeUploadedPacks();

        List<String> paths = result.stream().map(VirtualFile::getPath).toList();

        // Verify files were actually merged
        assertFalse(result.isEmpty(), "Should have merged files from nested pack");

        // Verify paths are normalized to start with assets/ (not the nested prefix)
        assertTrue(paths.stream().anyMatch(p -> p.startsWith("assets/minecraft/")),
                "Nested pack paths should be normalized to assets/minecraft/, got: " + paths);

        // Verify the nested prefix was stripped (paths should NOT contain the original
        // nesting)
        assertFalse(paths.stream().anyMatch(p -> p.contains("jars_battlepass")),
                "Nested prefix should be stripped from paths, got: " + paths);
        assertFalse(paths.stream().anyMatch(p -> p.contains("Textures (use 1)")),
                "Nested prefix should be stripped from paths, got: " + paths);

        // Verify expected files are present
        assertTrue(paths.contains("assets/minecraft/font/default.json"),
                "Should contain font file, got: " + paths);
        assertTrue(paths.contains("assets/minecraft/models/item/paper.json"),
                "Should contain model file, got: " + paths);
        assertTrue(paths.contains("assets/minecraft/textures/item/icons/test.png"),
                "Should contain texture file, got: " + paths);
    }

    @Test
    void testDetectAssetsRoot() throws IOException {
        // Test the detection of where 'assets' folder actually is in a zip
        Path nestedZip = uploadsFolder.resolve("nested.zip");
        createNestedPackZip(nestedZip, "some/deep/path");

        String assetsPrefix = detectAssetsPrefix(nestedZip);
        System.out.println("Detected assets prefix: '" + assetsPrefix + "'");

        assertEquals("some/deep/path/", assetsPrefix,
                "Should detect the path prefix before 'assets'");
    }

    /**
     * Detects the path prefix before the 'assets' folder in a zip file.
     * Returns empty string if assets is at root.
     */
    private String detectAssetsPrefix(Path zipFile) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                int assetsIndex = name.indexOf("assets/");
                if (assetsIndex >= 0) {
                    return name.substring(0, assetsIndex);
                }
                zis.closeEntry();
            }
        }
        return "";
    }

    /**
     * Creates a standard resource pack zip with assets at root.
     */
    private void createStandardPackZip(Path zipPath) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            addZipEntry(zos, "pack.mcmeta", "{\"pack\":{\"pack_format\":34,\"description\":\"Test\"}}");
            addZipEntry(zos, "assets/minecraft/font/default.json", "{\"providers\":[]}");
            addZipEntry(zos, "assets/minecraft/models/item/paper.json", "{\"parent\":\"item/generated\"}");
            addZipEntry(zos, "assets/minecraft/textures/item/icons/test.png", "FAKE_PNG_DATA");
        }
    }

    /**
     * Creates a nested pack zip like BattlePass has.
     */
    private void createNestedPackZip(Path zipPath, String prefix) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            String p = prefix.endsWith("/") ? prefix : prefix + "/";
            addZipEntry(zos, p + "pack.mcmeta", "{\"pack\":{\"pack_format\":34,\"description\":\"Test\"}}");
            addZipEntry(zos, p + "assets/minecraft/font/default.json",
                    "{\"providers\":[{\"type\":\"bitmap\",\"file\":\"minecraft:font/test.png\"}]}");
            addZipEntry(zos, p + "assets/minecraft/models/item/paper.json",
                    "{\"parent\":\"item/generated\",\"overrides\":[{\"predicate\":{\"custom_model_data\":1},\"model\":\"item/icons/test\"}]}");
            addZipEntry(zos, p + "assets/minecraft/textures/item/icons/test.png", "FAKE_PNG_DATA");
        }
    }

    private void addZipEntry(ZipOutputStream zos, String path, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(path));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    /**
     * A testable version of PackMerger that doesn't depend on Oraxen's logging
     * system.
     */
    static class TestablePackMerger {
        private final File uploadsDirectory;
        private static final String UPLOADS_DIR_NAME = "uploads";
        private final Map<String, String> fileOrigins = new LinkedHashMap<>();

        public TestablePackMerger(File packFolder) {
            this.uploadsDirectory = new File(packFolder, UPLOADS_DIR_NAME);
        }

        public List<VirtualFile> mergeUploadedPacks() {
            Map<String, VirtualFile> mergedFilesMap = new LinkedHashMap<>();
            fileOrigins.clear();

            if (!uploadsDirectory.exists()) {
                uploadsDirectory.mkdirs();
                return new ArrayList<>();
            }

            File[] uploadedPacks = uploadsDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".zip"));
            if (uploadedPacks == null || uploadedPacks.length == 0) {
                return new ArrayList<>();
            }

            for (File packZip : uploadedPacks) {
                if (!packZip.isFile() || !packZip.canRead()) {
                    System.out.println("Cannot read pack file: " + packZip.getName());
                    continue;
                }
                try {
                    mergePackZip(packZip, mergedFilesMap);
                } catch (IOException e) {
                    System.out.println("Failed to merge pack: " + packZip.getName());
                    e.printStackTrace();
                }
            }

            return new ArrayList<>(mergedFilesMap.values());
        }

        private void mergePackZip(File packZip, Map<String, VirtualFile> mergedFilesMap) throws IOException {
            String packName = packZip.getName();
            System.out.println("Processing pack: " + packName);

            // First pass: detect assets prefix
            String assetsPrefix = detectAssetsPrefix(packZip);
            System.out.println("Detected assets prefix: '" + assetsPrefix + "'");

            int fileCount = 0;
            try (FileInputStream fis = new FileInputStream(packZip);
                    ZipInputStream zis = new ZipInputStream(fis, StandardCharsets.UTF_8)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        zis.closeEntry();
                        continue;
                    }

                    String entryPath = entry.getName();

                    // Skip files not under the detected assets root
                    if (!assetsPrefix.isEmpty() && !entryPath.startsWith(assetsPrefix)) {
                        zis.closeEntry();
                        continue;
                    }

                    // Normalize the path by removing the prefix
                    String normalizedPath = entryPath;
                    if (!assetsPrefix.isEmpty()) {
                        normalizedPath = entryPath.substring(assetsPrefix.length());
                    }

                    // Skip if not a resource pack file (must be assets/, pack.mcmeta, or pack.png)
                    if (!normalizedPath.startsWith("assets/") &&
                            !normalizedPath.equals("pack.mcmeta") &&
                            !normalizedPath.equals("pack.png")) {
                        zis.closeEntry();
                        continue;
                    }

                    byte[] buffer = zis.readAllBytes();
                    VirtualFile virtualFile = new VirtualFile(
                            getParentFolder(normalizedPath),
                            getFileName(normalizedPath),
                            new ByteArrayInputStream(buffer));

                    String filePath = virtualFile.getPath();
                    mergedFilesMap.put(filePath, virtualFile);
                    fileOrigins.put(filePath, packName);
                    fileCount++;
                    zis.closeEntry();
                }
            }

            System.out.println("Added " + fileCount + " files from " + packName);
        }

        private String detectAssetsPrefix(File packZip) throws IOException {
            try (FileInputStream fis = new FileInputStream(packZip);
                    ZipInputStream zis = new ZipInputStream(fis, StandardCharsets.UTF_8)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String name = entry.getName();
                    int assetsIndex = name.indexOf("assets/");
                    if (assetsIndex >= 0) {
                        return name.substring(0, assetsIndex);
                    }
                    zis.closeEntry();
                }
            }
            return "";
        }

        private String getParentFolder(String path) {
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash < 0)
                return "";
            if (lastSlash == 0)
                return "";
            return path.substring(0, lastSlash);
        }

        private String getFileName(String path) {
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash < 0)
                return path;
            return path.substring(lastSlash + 1);
        }
    }
}
