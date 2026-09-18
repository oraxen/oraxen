package io.th0rgal.oraxen.pack.generation;

import io.th0rgal.oraxen.utils.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class UnprotectedPackWriterTest {
    @TempDir
    Path directory;

    @Test
    void exportsReadableFilesAndPreservesClientPackStreamsForObfuscation() throws Exception {
        String model = "{\"textures\":{\"layer0\":\"oraxen:custom/sword\"}}";
        List<VirtualFile> output = new ArrayList<>(List.of(
                file("assets/oraxen/models/custom", "sword.json", model),
                file("assets/oraxen/textures/custom", "sword.png", "texture bytes"),
                file("", "pack.mcmeta", "{\"pack\":{\"pack_format\":46,\"description\":\"Test\"}}")));
        Path destination = directory.resolve(".output/unprotected.zip");

        UnprotectedPackWriter.write(destination, output, directory.resolve("pack").toFile());
        PackObfuscator.obfuscate(output, "FULL", false);

        assertTrue(output.stream().noneMatch(file -> file.getPath().equals("assets/oraxen/models/custom/sword.json")));
        VirtualFile texture = output.stream().filter(file -> file.getPath().endsWith(".png")).findFirst().orElseThrow();
        assertEquals("texture bytes", new String(texture.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        try (ZipFile zip = new ZipFile(destination.toFile())) {
            assertEquals(3, zip.size());
            assertEquals(model, new String(zip.getInputStream(zip.getEntry("assets/oraxen/models/custom/sword.json")).readAllBytes(), StandardCharsets.UTF_8));
            assertNotNull(zip.getEntry("pack.mcmeta"));
        }
    }

    @Test
    void refreshesExistingExportAndSupportsSupplierStreams() throws Exception {
        Path destination = directory.resolve("unprotected.zip");
        VirtualFile file = new VirtualFile("", "pack.mcmeta", () -> new ByteArrayInputStream("new".getBytes(StandardCharsets.UTF_8)));
        Files.writeString(destination, "old");
        UnprotectedPackWriter.write(destination, List.of(file), directory.resolve("pack").toFile());
        try (ZipFile zip = new ZipFile(destination.toFile())) {
            assertEquals("new", new String(zip.getInputStream(zip.getEntry("pack.mcmeta")).readAllBytes(), StandardCharsets.UTF_8));
        }
        assertEquals("new", new String(file.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void rejectsClientPackAndSourceFolderDestinationsBeforeConsumingStreams() throws Exception {
        Path packFolder = directory.resolve("pack");
        VirtualFile file = file("", "pack.mcmeta", "content");
        for (String name : List.of("pack.zip", "pack_1_21_4.zip", "exports/unprotected.zip")) {
            assertThrows(java.io.IOException.class, () -> UnprotectedPackWriter.write(packFolder.resolve(name), List.of(file), packFolder.toFile()));
        }
        assertEquals("content", new String(file.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void failedExportKeepsPreviousArchiveAndRestoresStreams() throws Exception {
        Path destination = directory.resolve("unprotected.zip");
        Files.writeString(destination, "previous export");
        VirtualFile valid = file("", "pack.mcmeta", "content");
        VirtualFile invalid = new VirtualFile("", "missing", (java.io.InputStream) null);
        assertThrows(java.io.IOException.class, () -> UnprotectedPackWriter.write(destination, List.of(valid, invalid), directory.resolve("pack").toFile()));
        assertEquals("previous export", Files.readString(destination));
        assertEquals("content", new String(valid.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        try (var files = Files.list(directory)) {
            assertEquals(List.of(destination), files.toList());
        }
    }

    private static VirtualFile file(String parent, String name, String content) {
        return new VirtualFile(parent, name, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }
}
