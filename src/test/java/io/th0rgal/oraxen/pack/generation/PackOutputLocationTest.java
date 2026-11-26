package io.th0rgal.oraxen.pack.generation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PackOutputLocationTest {
    @TempDir
    Path directory;

    @Test
    void defaultKeepsLegacyLocation() throws Exception {
        File source = directory.resolve("pack").toFile();
        File expected = new File(source, "pack.zip");
        assertEquals(expected, PackOutputLocation.resolve(source, null));
        assertEquals(expected, PackOutputLocation.resolve(source, ""));
        assertEquals(expected, PackOutputLocation.resolve(source, "plugins/Oraxen/pack/pack.zip"));
    }

    @Test
    void resolvesCustomZipPaths() throws Exception {
        File source = directory.resolve("pack").toFile();
        Path output = directory.resolve("web/packs/custom.zip");
        assertEquals(output.toFile().getCanonicalFile(), PackOutputLocation.resolve(source, output.toString()));
    }

    @Test
    void rejectsInvalidAndNestedOutputs() throws Exception {
        File source = Files.createDirectory(directory.resolve("pack")).toFile();
        assertThrows(IOException.class, () -> PackOutputLocation.resolve(source, directory.resolve("custom").toString()));
        assertThrows(IOException.class, () -> PackOutputLocation.resolve(source, source.toPath().resolve("assets/custom.zip").toString()));
    }
}
