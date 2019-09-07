package io.th0rgal.oraxen.utils.pack;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class UnzipUtils {


    /**
     * Unpack an archive from a URL
     *
     * @param url
     * @param targetDir
     * @return the file to the url
     * @throws IOException
     */
    public static File unpackArchive(URL url, File targetDir) throws IOException {
        if (!targetDir.exists())
            targetDir.mkdirs();

        ReadableByteChannel readChannel = Channels.newChannel(url.openStream());
        File zip = File.createTempFile("pack", ".temp", targetDir);
        new FileOutputStream(zip)
                .getChannel()
                .transferFrom(readChannel, 0, Long.MAX_VALUE);

        return unpackArchive(zip, targetDir);
    }

    /**
     * Unpack a zip file
     *
     * @param theFile
     * @param targetDir
     * @return the file
     * @throws IOException
     */
    public static File unpackArchive(File theFile, File targetDir) throws IOException {
        if (!theFile.exists()) {
            throw new IOException(theFile.getAbsolutePath() + " does not exist");
        }
        if (buildDirectory(targetDir)) {
            throw new IOException("Could not create directory: " + targetDir);
        }
        ZipFile zipFile = new ZipFile(theFile);
        for (Enumeration entries = zipFile.entries(); entries.hasMoreElements(); ) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            File file = new File(targetDir, File.separator + entry.getName());
            if (buildDirectory(file.getParentFile())) {
                throw new IOException("Could not create directory: " + file.getParentFile());
            }
            if (!entry.isDirectory()) {
                copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(file)));
            } else {
                if (buildDirectory(file)) {
                    throw new IOException("Could not create directory: " + file);
                }
            }
        }
        zipFile.close();
        return theFile;
    }

    public static void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len = in.read(buffer);
        while (len >= 0) {
            out.write(buffer, 0, len);
            len = in.read(buffer);
        }
        in.close();
        out.close();
    }

    public static boolean buildDirectory(File file) {
        return !file.exists() && !file.mkdirs();
    }


}
