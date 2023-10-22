package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.OraxenPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtils {

    public static void extractDefaultZipPack() {
        Path destDirectory = OraxenPlugin.get().getDataFolder().toPath().resolve("pack");
        String zipFilePath = OraxenPlugin.get().getDataFolder().toPath().resolve("pack/DefaultPack.zip").toString();
        try {
            File destDir = destDirectory.toFile();
            // Create destination directory if it doesn't exist
            if (!destDir.exists()) {
                destDir.mkdirs();
            }

            byte[] buffer = new byte[1024];
            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath));
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
            System.out.println("ZIP folder extracted successfully.");

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error extracting ZIP folder.");
        }
    }
}
