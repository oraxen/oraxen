package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.config.Settings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    public static void writeZipFile(final File outputFile, final File directoryToZip,
                                    final List<VirtualFile> fileList) {
        try {
            final FileOutputStream fos = new FileOutputStream(outputFile);
            final ZipOutputStream zos = new ZipOutputStream(fos, StandardCharsets.UTF_8);
            final int compressionLevel = Deflater.class.getDeclaredField(Settings.COMPRESSION.toString()).getInt(null);
            zos.setLevel(compressionLevel);
            zos.setComment(Settings.COMMENT.toString());
            CustomArmorsTextures customArmorsTextures = new CustomArmorsTextures();
            for (final VirtualFile file : fileList)
                addToZip(file.getPath(),
                        file.getInputStream(),
                        zos);
            zos.close();
            fos.close();
        } catch (final IOException | NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static void addToZip(String zipFilePath, final InputStream fis, final ZipOutputStream zos)
            throws IOException {
        final ZipEntry zipEntry = new ZipEntry(zipFilePath);
        zipEntry.setLastModifiedTime(FileTime.fromMillis(0L));
        zos.putNextEntry(zipEntry);

        final byte[] bytes = new byte[1024];
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