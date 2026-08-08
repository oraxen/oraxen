package io.th0rgal.oraxen.api;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.pack.generation.ResourcePack;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.ZipUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OraxenPackTest {

    @Test
    void addFilesToPackKeepsFileContentForDeferredZipWriting(@TempDir Path tempDir) throws Exception {
        Field pluginField = OraxenPlugin.class.getDeclaredField("oraxen");
        pluginField.setAccessible(true);
        Object previousPlugin = pluginField.get(null);

        OraxenPlugin plugin = mock(OraxenPlugin.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        pluginField.set(null, plugin);

        Field outputFilesField = ResourcePack.class.getDeclaredField("outputFiles");
        outputFilesField.setAccessible(true);
        Object previousOutputFiles = outputFilesField.get(null);

        try {
            new ResourcePack();
            byte[] content = {0, 1, 2, 3, 4};
            Path source = tempDir.resolve("custom.bin");
            Files.write(source, content);

            OraxenPack.addFilesToPack(new File[]{source.toFile()});

            @SuppressWarnings("unchecked")
            Map<String, VirtualFile> outputFiles = (Map<String, VirtualFile>) outputFilesField.get(null);
            assertEquals(1, outputFiles.size());
            VirtualFile virtualFile = outputFiles.values().iterator().next();

            ByteArrayOutputStream zipBytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(zipBytes)) {
                ZipUtils.addToZip(virtualFile.getPath(), virtualFile.getInputStream(), zip);
            }

            try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes.toByteArray()))) {
                ZipEntry entry = zip.getNextEntry();
                assertNotNull(entry);
                assertEquals(virtualFile.getPath(), entry.getName());
                assertArrayEquals(content, zip.readAllBytes());
                assertNull(zip.getNextEntry());
            }
        } finally {
            outputFilesField.set(null, previousOutputFiles);
            pluginField.set(null, previousPlugin);
        }
    }
}
