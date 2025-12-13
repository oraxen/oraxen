package io.th0rgal.oraxen.pack.generation;

import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PackMerger {

    private final File uploadsDirectory;
    private static final String UPLOADS_DIR_NAME = "uploads";
    private final Map<String, String> fileOrigins = new LinkedHashMap<>();

    public PackMerger(File packFolder) {
        this.uploadsDirectory = new File(packFolder, UPLOADS_DIR_NAME);
    }

    @NotNull
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
                Logs.logWarning("Cannot read pack file: <yellow>" + packZip.getName() + "</yellow>");
                continue;
            }
            try {
                mergePackZip(packZip, mergedFilesMap);
            } catch (IOException e) {
                Logs.logError("Failed to merge pack: <red>" + packZip.getName() + "</red>");
                e.printStackTrace();
            }
        }

        List<VirtualFile> result = new ArrayList<>(mergedFilesMap.values());
        if (!result.isEmpty()) {
            Logs.logSuccess("Merged <blue>" + result.size() + "</blue> files from <blue>" + uploadedPacks.length + "</blue> uploaded pack(s)");
        }

        return result;
    }

    private void mergePackZip(File packZip, Map<String, VirtualFile> mergedFilesMap) throws IOException {
        String packName = packZip.getName();
        Logs.logInfo("Processing pack | <blue>" + packName + "</blue>");

        // First pass: detect all resource pack roots in the zip
        List<String> packRoots;
        try {
            packRoots = detectResourcePackRoots(packZip);
        } catch (Exception e) {
            Logs.logError("Failed to detect pack structure in <red>" + packName + "</red>: " + e.getMessage());
            return;
        }
        
        if (packRoots.isEmpty()) {
            Logs.logWarning("No valid resource pack structure found in <yellow>" + packName + "</yellow>");
            Logs.logWarning("Expected 'assets/' folder at root or nested within the zip");
            return;
        }
        
        if (packRoots.size() > 1) {
            Logs.logInfo("Found <blue>" + packRoots.size() + "</blue> resource pack(s) in <blue>" + packName + "</blue>");
        }

        int fileCount = 0;
        int overrideCount = 0;
        
        try (FileInputStream fis = new FileInputStream(packZip);
             ZipInputStream zis = new ZipInputStream(fis, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }

                String entryPath = entry.getName();
                
                // Find which pack root this entry belongs to
                String normalizedPath = normalizeEntryPath(entryPath, packRoots);
                if (normalizedPath == null) {
                    // Not a resource pack file, skip it
                    zis.closeEntry();
                    continue;
                }

                byte[] buffer = zis.readAllBytes();

                VirtualFile virtualFile = new VirtualFile(
                        getParentFolder(normalizedPath),
                        getFileName(normalizedPath),
                        new ByteArrayInputStream(buffer)
                );

                String filePath = virtualFile.getPath();
                if (mergedFilesMap.containsKey(filePath)) {
                    String previousPack = fileOrigins.get(filePath);
                    Logs.logWarning("<blue>" + packName + "</blue> will override existing file <yellow>" + filePath + "</yellow> from <red>" + previousPack + "</red>");
                    overrideCount++;
                }

                mergedFilesMap.put(filePath, virtualFile);
                fileOrigins.put(filePath, packName);
                fileCount++;
                zis.closeEntry();
            }
        }

        if (fileCount > 0) {
            if (overrideCount > 0) {
                Logs.logSuccess("Added <green>" + (fileCount - overrideCount) + "</green> new files, <yellow>" + overrideCount + "</yellow> overrides from <blue>" + packName + "</blue>");
            } else {
                Logs.logSuccess("Added <green>" + fileCount + "</green> files from <blue>" + packName + "</blue>");
            }
        }
    }

    /**
     * Detects all resource pack root prefixes in a zip file.
     * A resource pack root is the path prefix before 'assets/' or where pack.mcmeta is located.
     * 
     * For example, if a zip contains:
     * - "Textures/pack1/assets/minecraft/..." -> root is "Textures/pack1/"
     * - "assets/minecraft/..." -> root is ""
     * - "some/deep/path/pack.mcmeta" -> root is "some/deep/path/"
     * 
     * @param packZip the zip file to analyze
     * @return list of unique pack root prefixes found
     */
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
                
                // Also check for pack.mcmeta at various levels
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
        
        // Remove roots that are subdirectories of other roots
        // (e.g., if we have both "" and "subdir/", keep only "")
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

    /**
     * Normalizes an entry path by stripping the pack root prefix.
     * Returns null if the entry is not a valid resource pack file.
     * 
     * @param entryPath the original zip entry path
     * @param packRoots the detected pack root prefixes
     * @return normalized path starting with "assets/" or root files like "pack.mcmeta", or null if not a pack file
     */
    @Nullable
    private String normalizeEntryPath(String entryPath, List<String> packRoots) {
        for (String root : packRoots) {
            if (!entryPath.startsWith(root)) {
                continue;
            }
            
            String relativePath = entryPath.substring(root.length());
            
            // Valid resource pack files: assets/*, pack.mcmeta, pack.png
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
        if (lastSlash < 0) {
            return "";
        }
        if (lastSlash == 0) {
            return "";
        }
        return path.substring(0, lastSlash);
    }

    private String getFileName(String path) {
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash < 0) {
            return path;
        }
        return path.substring(lastSlash + 1);
    }

}
