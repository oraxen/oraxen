package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    public static void getAllFiles(File dir, List<File> fileList, String... blacklisted) {
        File[] files = dir.listFiles();
        List<String> blacklist = Arrays.asList(blacklisted);
        for (File file : files) {
            if (!blacklist.contains(file.getName()))
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

    public static void writeZipFile(File outputFile, File directoryToZip,
                                    Map<String, List<File>> fileListByZipDirectory) {

        try {
            FileOutputStream fos = new FileOutputStream(outputFile);
            ZipOutputStream zos = new ZipOutputStream(fos);

            int compressionLevel = Deflater.class.getDeclaredField(Settings.COMPRESSION.toString()).getInt(null);
            zos.setLevel(compressionLevel);
            zos.setComment(Settings.COMMENT.toString());

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

    public static void addToZip(File directoryToZip, File file, String inZipDirectory, ZipOutputStream zos)
            throws IOException {

        InputStream fis;

        if (file.getName().endsWith(".json")) {
            String content = Files.readString(Path.of(file.getPath()), StandardCharsets.UTF_8);
            String[] placeholders = OraxenPlugin.get().getFontManager().getMiniMessagePlaceholders();
            for (int i = 0; i < placeholders.length; i += 2)
                content = content.replace("<" + placeholders[i] + ">", placeholders[i + 1]);
            fis = new ByteArrayInputStream(content.getBytes());
        } else {
            fis = new FileInputStream(file);
        }

        // we want the zipEntry's path to be a relative path that is relative
        // to the directory being zipped, so chop off the rest of the path
        String zipFilePath = file.getCanonicalPath().substring(directoryToZip.getCanonicalPath().length() + 1);
        if (OS.getOs().getName().startsWith("Windows"))
            zipFilePath = zipFilePath.replace("\\", "/");
        if (!inZipDirectory.isEmpty())
            zipFilePath = inZipDirectory + "/" + zipFilePath;

        ZipEntry zipEntry = new ZipEntry(zipFilePath);
        zipEntry.setLastModifiedTime(FileTime.fromMillis(0L));
        zos.putNextEntry(zipEntry);

        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0)
            zos.write(bytes, 0, length);

        zos.closeEntry();
        fis.close();
        if (!(Settings.PROTECTION.toBool()))
            return;
        zipEntry.setCrc(bytes.length);
        zipEntry.setSize(new BigInteger(bytes).mod(BigInteger.valueOf(Long.MAX_VALUE)).longValue());
    }

}
