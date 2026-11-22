package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.ConfigsManager;
import io.th0rgal.oraxen.configs.Settings;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ZipUtilsTest {
    @TempDir
    Path temporaryDirectory;

    private final YamlConfiguration settings = new YamlConfiguration();
    private MockedStatic<OraxenPlugin> plugins;
    private final Map<String, byte[]> contents = new LinkedHashMap<>();

    @BeforeEach
    void configurePackWriter() {
        OraxenPlugin plugin = mock(OraxenPlugin.class);
        ConfigsManager configs = mock(ConfigsManager.class);
        when(plugin.getConfigsManager()).thenReturn(configs);
        when(configs.getSettings()).thenReturn(settings);
        plugins = mockStatic(OraxenPlugin.class);
        plugins.when(OraxenPlugin::get).thenReturn(plugin);
        settings.set(Settings.COMPRESSION.getPath(), "BEST_COMPRESSION");
        settings.set(Settings.COMMENT.getPath(), "Pack writer integration test");
        settings.set(Settings.PACK_SIZE_SPOOFING.getPath(), false);
        contents.put("pack.mcmeta", "{\"pack\":{\"description\":\"Test pack\"}}".getBytes(StandardCharsets.UTF_8));
        contents.put("assets/test/textures/example.bin", new byte[]{0, 1, 2, 3, -1, -128});
        Settings.invalidateCache();
    }

    @AfterEach
    void clearSettings() {
        Settings.invalidateCache();
        plugins.close();
    }

    @Test
    void disabledSpoofingProducesExtractablePack() throws Exception {
        Path output = writePack("ordinary.zip");
        int entries = 0;
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(output))) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                assertArrayEquals(contents.get(entry.getName()), zip.readAllBytes());
                assertEquals(contents.get(entry.getName()).length, entry.getSize());
                entries++;
            }
        }
        assertEquals(contents.size(), entries);
        assertDescriptorFlag(output, true);
    }

    @Test
    void enabledSpoofingPreservesFullJavaZipFileReads() throws Exception {
        settings.set(Settings.PACK_SIZE_SPOOFING.getPath(), true);
        Settings.invalidateCache();
        Path output = writePack("protected.zip");
        try (ZipFile zip = new ZipFile(output.toFile())) {
            assertEquals("Pack writer integration test", zip.getComment());
            assertEquals(contents.size(), zip.size());
            for (var expected : contents.entrySet()) {
                var entry = zip.getEntry(expected.getKey());
                assertNotNull(entry);
                assertEquals(0, entry.getSize());
                assertTrue(entry.getCompressedSize() > 0);
                try (var stream = zip.getInputStream(entry)) {
                    assertArrayEquals(expected.getValue(), stream.readAllBytes());
                }
            }
        }
        assertDescriptorFlag(output, false);
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(output))) {
            assertNotNull(zip.getNextEntry());
            assertThrows(ZipException.class, zip::readAllBytes);
        }
    }

    private Path writePack(String filename) {
        List<VirtualFile> files = contents.entrySet().stream()
                .map(entry -> new VirtualFile("", entry.getKey(), () -> new ByteArrayInputStream(entry.getValue())))
                .toList();
        Path output = temporaryDirectory.resolve(filename);
        ZipUtils.writeZipFile(output.toFile(), files);
        return output;
    }

    private void assertDescriptorFlag(Path output, boolean enabled) throws Exception {
        byte[] bytes = Files.readAllBytes(output);
        ByteBuffer headers = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int end = bytes.length - 22 - "Pack writer integration test".getBytes(StandardCharsets.UTF_8).length;
        assertEquals(0x06054b50, headers.getInt(end));
        int central = headers.getInt(end + 16);
        assertEquals(enabled, (headers.getShort(6) & 8) != 0);
        assertEquals(enabled, (headers.getShort(central + 8) & 8) != 0);
    }
}