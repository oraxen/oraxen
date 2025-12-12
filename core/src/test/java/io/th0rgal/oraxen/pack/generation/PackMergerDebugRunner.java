package io.th0rgal.oraxen.pack.generation;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Standalone CLI tool to debug pack merging issues.
 * 
 * Run with: ./gradlew :core:runPackMergerDebug -PpackFile=path/to/pack.zip
 * 
 * Or build and run directly:
 *   javac -d out core/src/test/java/io/th0rgal/oraxen/pack/generation/PackMergerDebugRunner.java
 *   java -cp out io.th0rgal.oraxen.pack.generation.PackMergerDebugRunner path/to/pack.zip
 */
public class PackMergerDebugRunner {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: PackMergerDebugRunner <pack.zip> [output_folder]");
            System.out.println();
            System.out.println("Analyzes a resource pack zip and shows how it would be merged.");
            System.out.println();
            
            // If no args, try to find BattlePass in workspace
            Path battlePass = Path.of("BattlePass-v1.2.0.zip");
            if (Files.exists(battlePass)) {
                System.out.println("Found BattlePass-v1.2.0.zip in current directory, analyzing...");
                args = new String[]{battlePass.toString()};
            } else {
                return;
            }
        }

        Path packZip = Path.of(args[0]);
        Path outputFolder = args.length > 1 ? Path.of(args[1]) : null;

        if (!Files.exists(packZip)) {
            System.err.println("Error: File not found: " + packZip);
            System.exit(1);
        }

        System.out.println("=".repeat(60));
        System.out.println("Pack Merger Debug Analysis");
        System.out.println("=".repeat(60));
        System.out.println("Input: " + packZip.toAbsolutePath());
        System.out.println();

        analyzeZipStructure(packZip);
        System.out.println();

        List<MergeResult> results = simulateMerge(packZip);
        System.out.println();

        if (outputFolder != null) {
            System.out.println("Writing normalized files to: " + outputFolder);
            writeNormalizedFiles(results, outputFolder);
        }

        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("Summary");
        System.out.println("=".repeat(60));

        boolean hasIssues = results.stream().anyMatch(r -> r.issue != null);
        if (hasIssues) {
            System.out.println("⚠ ISSUES DETECTED:");
            results.stream()
                    .filter(r -> r.issue != null)
                    .forEach(r -> System.out.println("  - " + r.issue));
        } else {
            System.out.println("✓ No issues detected. Pack should merge correctly.");
        }
    }

    static void analyzeZipStructure(Path zipPath) throws IOException {
        System.out.println("--- Zip Structure Analysis ---");
        
        Map<String, Integer> rootFolders = new LinkedHashMap<>();
        Set<String> assetsRoots = new LinkedHashSet<>();
        int totalFiles = 0;

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    totalFiles++;
                }
                
                String name = entry.getName();
                
                // Track root folders
                int firstSlash = name.indexOf('/');
                if (firstSlash > 0) {
                    String root = name.substring(0, firstSlash);
                    rootFolders.merge(root, 1, Integer::sum);
                }
                
                // Find assets folders
                int assetsIndex = name.indexOf("assets/");
                if (assetsIndex >= 0) {
                    String prefix = name.substring(0, assetsIndex);
                    assetsRoots.add(prefix.isEmpty() ? "(root)" : prefix);
                }
                
                zis.closeEntry();
            }
        }

        System.out.println("Total files: " + totalFiles);
        System.out.println();
        System.out.println("Root folders:");
        rootFolders.forEach((folder, count) -> 
            System.out.println("  " + folder + "/ (" + count + " entries)"));

        System.out.println();
        System.out.println("Assets folder locations:");
        if (assetsRoots.isEmpty()) {
            System.out.println("  ⚠ No 'assets/' folder found!");
        } else {
            for (String root : assetsRoots) {
                System.out.println("  " + (root.equals("(root)") ? "assets/" : root + "assets/"));
            }
        }

        if (assetsRoots.size() > 1) {
            System.out.println("  ⚠ Multiple assets folders detected! This may cause issues.");
        }
        
        if (!assetsRoots.contains("(root)") && !assetsRoots.isEmpty()) {
            System.out.println();
            System.out.println("  ⚠ Assets folder is NOT at root level!");
            System.out.println("  ⚠ The current PackMerger will use wrong paths.");
        }
    }

    static List<MergeResult> simulateMerge(Path zipPath) throws IOException {
        System.out.println("--- Merge Simulation (WITH FIX) ---");
        
        List<MergeResult> results = new ArrayList<>();
        
        // Use the FIXED logic: detect all pack roots
        List<String> packRoots = detectAllPackRoots(zipPath);
        
        System.out.println("Detected pack roots:");
        for (String root : packRoots) {
            System.out.println("  - '" + root + "'");
        }
        System.out.println();

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }

                String originalPath = entry.getName();
                MergeResult result = new MergeResult();
                result.originalPath = originalPath;

                // Use the FIXED normalization logic
                String normalizedPath = normalizeEntryPath(originalPath, packRoots);
                result.correctedPath = normalizedPath;
                result.currentMergePath = normalizedPath; // Fixed behavior matches corrected

                if (normalizedPath == null) {
                    result.issue = "File not part of resource pack (skipped): " + originalPath;
                }

                byte[] content = zis.readAllBytes();
                result.size = content.length;
                result.content = content;

                results.add(result);
                zis.closeEntry();
            }
        }

        // Print results
        System.out.println("Files to merge:");
        int correct = 0, skipped = 0;
        for (MergeResult r : results) {
            if (r.correctedPath == null) {
                skipped++;
                System.out.println("  [SKIP] " + r.originalPath);
            } else {
                correct++;
                System.out.println("  [OK]   " + r.correctedPath + " (from: " + r.originalPath + ")");
            }
        }

        System.out.println();
        System.out.println("Results: " + correct + " merged, " + skipped + " skipped");

        return results;
    }

    static List<String> detectAllPackRoots(Path zipPath) throws IOException {
        Set<String> roots = new LinkedHashSet<>();
        
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                
                int assetsIndex = name.indexOf("assets/");
                if (assetsIndex >= 0) {
                    String root = name.substring(0, assetsIndex);
                    roots.add(root);
                }
                
                if (name.endsWith("pack.mcmeta")) {
                    String parent = getParentFolder(name);
                    if (!parent.isEmpty()) {
                        parent = parent + "/";
                    }
                    roots.add(parent);
                }
                
                zis.closeEntry();
            }
        }
        
        // Remove roots that are subdirectories of other roots
        List<String> sortedRoots = new ArrayList<>(roots);
        sortedRoots.sort(Comparator.comparingInt(String::length));
        
        List<String> finalRoots = new ArrayList<>();
        for (String root : sortedRoots) {
            boolean isSubdirOfExisting = false;
            for (String existing : finalRoots) {
                if (root.startsWith(existing)) {
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

    static String normalizeEntryPath(String entryPath, List<String> packRoots) {
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

    static String getParentFolder(String path) {
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash < 0) return "";
        if (lastSlash == 0) return "";
        return path.substring(0, lastSlash);
    }

    static String detectAssetsPrefix(Path zipPath) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath), StandardCharsets.UTF_8)) {
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

    static void writeNormalizedFiles(List<MergeResult> results, Path outputFolder) throws IOException {
        Files.createDirectories(outputFolder);
        
        for (MergeResult r : results) {
            if (r.correctedPath == null) continue;
            
            Path outPath = outputFolder.resolve(r.correctedPath);
            Files.createDirectories(outPath.getParent());
            Files.write(outPath, r.content);
        }
        
        // Also create a proper zip
        Path zipOut = outputFolder.resolve("normalized_pack.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipOut))) {
            for (MergeResult r : results) {
                if (r.correctedPath == null) continue;
                zos.putNextEntry(new ZipEntry(r.correctedPath));
                zos.write(r.content);
                zos.closeEntry();
            }
        }
        System.out.println("Created normalized pack: " + zipOut);
    }

    static class MergeResult {
        String originalPath;
        String currentMergePath;
        String correctedPath;
        String issue;
        int size;
        byte[] content;
    }
}

