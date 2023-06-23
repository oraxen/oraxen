package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.OraxenPlugin;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JarReader {

    public static boolean checkIsLeaked() {
        JarFile jarFile = OraxenPlugin.getJarFile();
        if (jarFile == null) return false;
        Enumeration<JarEntry> entries = jarFile.entries();
        Pattern pattern = Pattern.compile("(module-info|DirectLea.*).class");

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();

            Matcher matcher = pattern.matcher(entryName);
            if (matcher.matches()) return true;
        }
        return false;
    }

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
