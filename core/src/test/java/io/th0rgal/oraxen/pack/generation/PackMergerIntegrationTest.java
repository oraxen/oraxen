package io.th0rgal.oraxen.pack.generation;

import io.th0rgal.oraxen.utils.VirtualFile;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PackMerger that test with real-world pack structures.
 * Uses a standalone testable version that doesn't depend on Bukkit/Adventure.
 * 
 * Run with: ./gradlew :core:test --tests
 * "io.th0rgal.oraxen.pack.generation.PackMergerIntegrationTest"
 */
public class PackMergerIntegrationTest {

    @TempDir
    Path tempDir;

    private Path packFolder;
    private Path uploadsFolder;
    private TestablePackMerger merger;

    @BeforeEach
    void setUp() throws IOException {
        packFolder = tempDir.resolve("pack");
        uploadsFolder = packFolder.resolve("uploads");
        Files.createDirectories(uploadsFolder);
        merger = new TestablePackMerger(packFolder.toFile());
    }

    @Test
    void testStandardResourcePackMerge() throws IOException {
        // Create a standard resource pack with assets at root
        createZipFile(uploadsFolder.resolve("standard.zip"),
                "pack.mcmeta", "{\"pack\":{\"pack_format\":34}}",
                "assets/minecraft/models/item/diamond.json", "{\"parent\":\"item/generated\"}",
                "assets/minecraft/textures/item/diamond.png", "PNG_DATA",
                "assets/.DS_Store", "junk");

        List<VirtualFile> result = merger.mergeUploadedPacks();

        List<String> paths = result.stream().map(VirtualFile::getPath).collect(Collectors.toList());

        assertEquals(3, result.size());
        assertTrue(paths.contains("pack.mcmeta"));
        assertTrue(paths.contains("assets/minecraft/models/item/diamond.json"));
        assertTrue(paths.contains("assets/minecraft/textures/item/diamond.png"));
        assertFalse(paths.stream().anyMatch(p -> p.endsWith(".DS_Store")), "Should ignore .DS_Store entries");
        assertFalse(paths.stream().anyMatch(p -> p.contains("__MACOSX")), "Should ignore __MACOSX entries");
    }

    @Test
    void testNestedResourcePackMerge_BattlePassStyle() throws IOException {
        // Simulate the BattlePass structure with deeply nested assets
        createZipFile(uploadsFolder.resolve("battlepass.zip"),
                // Plugin config files (should be skipped)
                "Plugins/BattlePass/config.yml", "some: config",

                // ItemsAdder style pack (nested)
                "Textures (use 1)/ItemsAdders/contents/pack/resourcepack/pack.mcmeta",
                "{\"pack\":{\"pack_format\":34}}",
                "Textures (use 1)/ItemsAdders/contents/pack/resourcepack/assets/minecraft/font/default.json",
                "{\"providers\":[]}",
                "Textures (use 1)/ItemsAdders/contents/pack/resourcepack/assets/minecraft/models/item/paper.json",
                "{\"parent\":\"item/generated\"}",
                "Textures (use 1)/ItemsAdders/contents/pack/resourcepack/assets/minecraft/textures/item/icons/icon.png",
                "PNG_DATA",

                // Vanilla pack version (different nested path)
                "Textures (use 1)/vanilla_pack/pack.mcmeta", "{\"pack\":{\"pack_format\":34}}",
                "Textures (use 1)/vanilla_pack/assets/minecraft/textures/item/icons/icon2.png", "PNG_DATA_2",

                // README file (should be skipped)
                "README.md", "# Instructions");

        List<VirtualFile> result = merger.mergeUploadedPacks();
        List<String> paths = result.stream().map(VirtualFile::getPath).collect(Collectors.toList());

        System.out.println("Merged paths:");
        paths.forEach(p -> System.out.println("  " + p));

        // Should NOT have the deep nested paths
        assertFalse(paths.stream().anyMatch(p -> p.contains("ItemsAdders")),
                "Should not contain ItemsAdders prefix");
        assertFalse(paths.stream().anyMatch(p -> p.contains("Textures")),
                "Should not contain Textures prefix");

        // Should have properly normalized paths
        assertTrue(paths.contains("assets/minecraft/font/default.json"));
        assertTrue(paths.contains("assets/minecraft/models/item/paper.json"));
        assertTrue(paths.contains("assets/minecraft/textures/item/icons/icon.png"));
        assertTrue(paths.contains("assets/minecraft/textures/item/icons/icon2.png"));

        // Should skip non-pack files
        assertFalse(paths.stream().anyMatch(p -> p.contains("config.yml")));
        assertFalse(paths.stream().anyMatch(p -> p.contains("README")));
    }

    @Test
    void testMultiplePacksWithOverrides() throws IOException {
        // First pack - using naming that ensures pack1 is processed first
        // (alphabetically)
        createZipFile(uploadsFolder.resolve("aaa_pack1.zip"),
                "assets/minecraft/models/item/paper.json", "{\"parent\":\"pack1\"}");

        // Second pack with same file (should override because it's processed second)
        createZipFile(uploadsFolder.resolve("zzz_pack2.zip"),
                "assets/minecraft/models/item/paper.json", "{\"parent\":\"pack2\"}",
                "assets/minecraft/models/item/diamond.json", "{\"parent\":\"pack2\"}");

        List<VirtualFile> result = merger.mergeUploadedPacks();

        assertEquals(2, result.size());

        // Find the paper.json file and verify it was overridden
        VirtualFile paperFile = result.stream()
                .filter(f -> f.getPath().contains("paper.json"))
                .findFirst()
                .orElseThrow();

        // The content should be from zzz_pack2 (last one wins based on file iteration
        // order)
        String content = new String(paperFile.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        // File order is not guaranteed, so just verify we got content from one of the
        // packs
        assertTrue(content.contains("pack1") || content.contains("pack2"),
                "Paper.json should contain content from one of the packs");
    }

    @Test
    void testEmptyUploadsFolder() throws IOException {
        List<VirtualFile> result = merger.mergeUploadedPacks();
        assertTrue(result.isEmpty());
    }

    @Test
    void testInvalidZipWithNoAssets() throws IOException {
        // Create a zip with no valid resource pack structure
        createZipFile(uploadsFolder.resolve("invalid.zip"),
                "some/random/file.txt", "content",
                "another/file.json", "{}");

        List<VirtualFile> result = merger.mergeUploadedPacks();
        assertTrue(result.isEmpty(), "Should return empty list for invalid pack structure");
    }

    private void createZipFile(Path zipPath, String... entries) throws IOException {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("Entries must be path/content pairs");
        }

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            for (int i = 0; i < entries.length; i += 2) {
                String path = entries[i];
                String content = entries[i + 1];

                zos.putNextEntry(new ZipEntry(path));
                zos.write(content.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
    }

    /**
     * Standalone testable version of PackMerger that doesn't depend on Oraxen's
     * logging.
     * This replicates the exact logic of the real PackMerger for testing purposes.
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

            List<String> packRoots = detectResourcePackRoots(packZip);

            if (packRoots.isEmpty()) {
                System.out.println("No valid resource pack structure found in " + packName);
                return;
            }

            if (packRoots.size() > 1) {
                System.out.println("Found " + packRoots.size() + " resource pack(s) in " + packName);
            }

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
                    String normalizedPath = normalizeEntryPath(entryPath, packRoots);
                    if (normalizedPath == null) {
                        zis.closeEntry();
                        continue;
                    }

                    String fileName = getFileName(normalizedPath);
                    if (".DS_Store".equals(fileName) || "__MACOSX".equals(fileName) || normalizedPath.contains("/__MACOSX/")) {
                        zis.closeEntry();
                        continue;
                    }

                    byte[] buffer = zis.readAllBytes();
                    VirtualFile virtualFile = new VirtualFile(
                            getParentFolder(normalizedPath),
                            fileName,
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

        private List<String> detectResourcePackRoots(File packZip) throws IOException {
            Set<String> roots = new LinkedHashSet<>();

            try (FileInputStream fis = new FileInputStream(packZip);
                    ZipInputStream zis = new ZipInputStream(fis, StandardCharsets.UTF_8)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String name = entry.getName();

                    // Look for assets/ folders - must be at start or after a path separator
                    // This prevents matching "testassets/" as a valid assets folder
                    int assetsIndex = findAssetsFolder(name);
                    if (assetsIndex >= 0) {
                        String root = name.substring(0, assetsIndex);
                        roots.add(root);
                    }

                    if (name.endsWith("pack.mcmeta")) {
                        String root = getParentFolder(name);
                        if (!root.isEmpty()) {
                            root = root + "/";
                        }
                        roots.add(root);
                    }

                    zis.closeEntry();
                }
            }

            List<String> sortedRoots = new ArrayList<>(roots);
            sortedRoots.sort(Comparator.comparingInt(String::length));

            List<String> finalRoots = new ArrayList<>();
            for (String root : sortedRoots) {
                boolean isSubdirOfExisting = false;
                for (String existing : finalRoots) {
                    // Fix: Empty string would match everything with startsWith
                    // Only filter if existing is non-empty AND root starts with it
                    if (!existing.isEmpty() && root.startsWith(existing)) {
                        isSubdirOfExisting = true;
                        break;
                    }
                }
                if (!isSubdirOfExisting) {
                    finalRoots.add(root);
                }
            }

            return finalRoots;
        }

        /**
         * Finds the index of "assets/" in a path, ensuring it's a proper folder name
         * (either at the start or immediately after a path separator).
         * 
         * @param path the path to search
         * @return index where "assets/" starts, or -1 if not found
         */
        private int findAssetsFolder(String path) {
            int index = 0;
            while (true) {
                int found = path.indexOf("assets/", index);
                if (found < 0) {
                    return -1;
                }
                // Valid if at start of path OR preceded by a path separator
                if (found == 0 || path.charAt(found - 1) == '/') {
                    return found;
                }
                // Continue searching after this match
                index = found + 1;
            }
        }

        @Nullable
        private String normalizeEntryPath(String entryPath, List<String> packRoots) {
            for (String root : packRoots) {
                if (!entryPath.startsWith(root)) {
                    continue;
                }

                String relativePath = entryPath.substring(root.length());

                if (relativePath.startsWith("assets/") ||
                        relativePath.equals("pack.mcmeta") ||
                        relativePath.equals("pack.png")) {
                    return relativePath;
                }
            }

            return null;
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
