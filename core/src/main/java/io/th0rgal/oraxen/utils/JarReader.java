package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.OraxenPlugin;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarReader {

    public static String getManifestContent() {
        JarFile jarFile = OraxenPlugin.getJarFile();
        if (jarFile == null) return "";
        Enumeration<JarEntry> entries = jarFile.entries();
        String manifest = "";

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();
            if (!entryName.contains("MANIFEST")) continue;
            InputStream manifestStream;
            try {
                manifestStream = jarFile.getInputStream(entry);
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }

            try {
                manifest = IOUtils.toString(manifestStream, StandardCharsets.UTF_8);
            } catch (IOException ignored) {
            }
        }
        return manifest;
    }
}
