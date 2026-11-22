package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.OraxenPlugin;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class JarReader {

    public static String getManifestContent() {
        if (OraxenPlugin.get() == null) return "";
        try (JarFile jarFile = OraxenPlugin.getJarFile()) {
            if (jarFile == null) return "";
            JarEntry entry = jarFile.getJarEntry(JarFile.MANIFEST_NAME);
            if (entry == null) return "";
            try (InputStream manifestStream = jarFile.getInputStream(entry)) {
                return new String(manifestStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
            return "";
        }
    }

    public static Attributes getManifestAttributes() {
        try (InputStream stream = new ByteArrayInputStream(getManifestContent().getBytes(StandardCharsets.UTF_8))) {
            return new Manifest(stream).getMainAttributes();
        } catch (IOException ignored) {
            return new Attributes();
        }
    }
}
