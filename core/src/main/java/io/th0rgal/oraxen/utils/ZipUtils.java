package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.pack.PackGenerator;
import io.th0rgal.oraxen.utils.logs.Logs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtils {

    public static void extractDefaultZipPack() {
        Path destDirectory = PackGenerator.externalPacks.resolve("DefaultPack").toFile().toPath();
        File zipFile = PackGenerator.externalPacks.resolve("DefaultPack.zip").toFile();
        if (!zipFile.exists()) return;

        File[] assets = destDirectory.resolve("assets").toFile().listFiles();
        if (assets != null && assets.length > 0) return;
        Logs.logInfo("Extracting default assets...");
        try {
            File destDir = destDirectory.toFile();
            // Create destination directory if it doesn't exist
            Files.createDirectory(destDirectory);

            byte[] buffer = new byte[1024];
            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry entry = zipInputStream.getNextEntry();

            while (entry != null) {
                String filePath = destDirectory + File.separator + entry.getName();

                // If the entry is a directory, create the directory
                if (entry.isDirectory()) {
                    File dir = new File(filePath);
                    dir.mkdirs();
                } else {
                    // If the entry is a file, extract it
                    FileOutputStream fos = new FileOutputStream(filePath);
                    int len;
                    while ((len = zipInputStream.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
                zipInputStream.closeEntry();
                entry = zipInputStream.getNextEntry();
            }

            zipInputStream.close();

        } catch (IOException e) {
            Logs.logWarning(e.getMessage());
        }
    }
}
