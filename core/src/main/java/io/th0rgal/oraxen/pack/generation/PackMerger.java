package io.th0rgal.oraxen.pack.generation;

import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.jetbrains.annotations.NotNull;

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
                byte[] buffer = zis.readAllBytes();

                VirtualFile virtualFile = new VirtualFile(
                        getParentFolder(entryPath),
                        getFileName(entryPath),
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
