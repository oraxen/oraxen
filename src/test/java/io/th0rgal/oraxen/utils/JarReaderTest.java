package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.OraxenPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JarReaderTest {
    @TempDir
    Path tempDir;

    private JarFile jarWithManifest(String content) throws IOException {
        Path path = tempDir.resolve("plugin.jar");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(path))) {
            if (content != null) {
                out.putNextEntry(new ZipEntry(JarFile.MANIFEST_NAME));
                out.write(content.getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
        return new JarFile(path.toFile());
    }

    private MockedStatic<OraxenPlugin> pluginWithJar(JarFile jar) {
        MockedStatic<OraxenPlugin> plugin = mockStatic(OraxenPlugin.class);
        plugin.when(OraxenPlugin::get).thenReturn(mock(OraxenPlugin.class));
        plugin.when(OraxenPlugin::getJarFile).thenReturn(jar);
        return plugin;
    }

    @Test
    void parsesMainAttributesIncludingContinuationsAndClosesJar() throws Exception {
        String content = "Manifest-Version: 1.0\r\ncompiled: true\r\nBuilt-By: si\r\n vert\r\n"
                + "Build-Timestamp: 12:34:56\r\n\r\nName: section\r\nCompiled: false\r\n\r\n";
        JarFile jar = jarWithManifest(content);
        try (MockedStatic<OraxenPlugin> plugin = pluginWithJar(jar)) {
            Attributes attributes = JarReader.getManifestAttributes();
            assertEquals("true", attributes.getValue("Compiled"));
            assertEquals("sivert", attributes.getValue("Built-By"));
            assertEquals("12:34:56", attributes.getValue("Build-Timestamp"));
            plugin.verify(OraxenPlugin::getJarFile, times(1));
        }
        assertThrows(IllegalStateException.class, jar::size);
    }

    @Test
    void missingManifestClosesJarAndReturnsEmptyAttributes() throws Exception {
        JarFile jar = jarWithManifest(null);
        try (MockedStatic<OraxenPlugin> plugin = pluginWithJar(jar)) {
            assertTrue(JarReader.getManifestAttributes().isEmpty());
        }
        assertThrows(IllegalStateException.class, jar::size);
    }

    @Test
    void readFailureClosesBothResources() throws Exception {
        JarFile jar = mock(JarFile.class);
        JarEntry entry = new JarEntry(JarFile.MANIFEST_NAME);
        ByteArrayInputStream stream = mock(ByteArrayInputStream.class);
        when(jar.getJarEntry(JarFile.MANIFEST_NAME)).thenReturn(entry);
        when(jar.getInputStream(entry)).thenReturn(stream);
        when(stream.readAllBytes()).thenThrow(new IOException("read failure"));
        try (MockedStatic<OraxenPlugin> plugin = pluginWithJar(jar)) {
            assertEquals("", JarReader.getManifestContent());
        }
        verify(stream).close();
        verify(jar).close();
    }

    @Test
    void malformedManifestReturnsEmptyAttributesAndClosesJar() throws Exception {
        JarFile jar = jarWithManifest("Compiled: true\ninvalid header\n");
        try (MockedStatic<OraxenPlugin> plugin = pluginWithJar(jar)) {
            assertTrue(JarReader.getManifestAttributes().isEmpty());
        }
        assertThrows(IllegalStateException.class, jar::size);
    }
}
