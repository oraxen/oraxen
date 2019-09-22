package io.th0rgal.oraxen.utils.pack;

import io.th0rgal.oraxen.settings.Pack;
import io.th0rgal.oraxen.settings.Server;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
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

    private final static int BUFFER_SIZE = 2048;

    public static boolean unzipToFile(File file, File outputDirectory) {
        try {
            BufferedInputStream bufIS = null;

            outputDirectory.mkdirs();

            // open archive for reading
            ZipFile zipFile = new ZipFile(file, ZipFile.OPEN_READ);

            //for every zip archive entry do
            Enumeration<? extends ZipEntry> zipFileEntries = zipFile.entries();
            while (zipFileEntries.hasMoreElements()) {
                ZipEntry entry = zipFileEntries.nextElement();
                System.out.println("\tExtracting entry: " + entry);

                //create destination file
                File destFile = new File(outputDirectory, entry.getName());

                //create parent directories if needed
                File parentDestFile = destFile.getParentFile();
                parentDestFile.mkdirs();

                if (!entry.isDirectory()) {
                    bufIS = new BufferedInputStream(
                            zipFile.getInputStream(entry));
                    int currentByte;

                    // buffer for writing file
                    byte[] data = new byte[BUFFER_SIZE];

                    // write the current file to disk
                    FileOutputStream fOS = new FileOutputStream(destFile);
                    BufferedOutputStream bufOS = new BufferedOutputStream(fOS, BUFFER_SIZE);

                    while ((currentByte = bufIS.read(data, 0, BUFFER_SIZE)) != -1)
                        bufOS.write(data, 0, currentByte);

                    // close BufferedOutputStream
                    bufOS.flush();
                    bufOS.close();
                }
            }
            bufIS.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
