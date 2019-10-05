package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.settings.Pack;
import io.th0rgal.oraxen.settings.Server;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    public static void getAllFiles(File dir, List<File> fileList, String... blacklisted) {
        File[] files = dir.listFiles();
        for (File file : files) {
            if (!Arrays.asList(blacklisted).contains(file.getName()))
                fileList.add(file);
            if (file.isDirectory())
                getAllFiles(file, fileList);
        }
    }

    public static void getFilesInFolder(File dir, List<File> fileList, String... blacklisted) {
        File[] files = dir.listFiles();
        for (File file : files)
            if (!file.isDirectory() && !Arrays.asList(blacklisted).contains(file.getName()))
                fileList.add(file);
    }

    public static void writeZipFile(File outputFile, File directoryToZip, Map<String, List<File>> fileListByZipDirectory) {

        try {
            FileOutputStream fos = new FileOutputStream(outputFile.getPath() + File.separator + directoryToZip.getName() + ".zip");
            ZipOutputStream zos = new ZipOutputStream(fos);

            int compressionLevel = Deflater.class.getDeclaredField(Pack.COMPRESSION.toString()).getInt(null);
            zos.setLevel(compressionLevel);
            zos.setComment(Pack.COMMENT.toString());

            for (Map.Entry<String, List<File>> inZipDirectoryFiles : fileListByZipDirectory.entrySet())
                for (File file : inZipDirectoryFiles.getValue())
                    if (!file.isDirectory()) // we only zip files, not directories
                        addToZip(directoryToZip, file, inZipDirectoryFiles.getKey(), zos);

            zos.close();
            fos.close();
        } catch (IOException | NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static void addToZip(File directoryToZip, File file, String inZipDirectory, ZipOutputStream zos) throws IOException {

        FileInputStream fis = new FileInputStream(file);

        // we want the zipEntry's path to be a relative path that is relative
        // to the directory being zipped, so chop off the rest of the path
        String zipFilePath = file.getCanonicalPath().substring(directoryToZip.getCanonicalPath().length() + 1);
        if (Server.isIsUsingWindows())
            zipFilePath = zipFilePath.replace("\\", "/");
        if (!inZipDirectory.isEmpty())
            zipFilePath = inZipDirectory + "/" + zipFilePath;

        ZipEntry zipEntry = new ZipEntry(zipFilePath);
        zos.putNextEntry(zipEntry);

        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0)
            zos.write(bytes, 0, length);

        zos.closeEntry();
        fis.close();
    }

}
