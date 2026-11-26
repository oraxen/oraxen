package io.th0rgal.oraxen.pack.generation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PackOutputLocationTest {
    @TempDir
    Path directory;

    @Test
    void defaultKeepsLegacyLocation() throws Exception {
        File source = directory.resolve("pack").toFile();
        File expected = new File(source, "pack.zip");
        assertEquals(expected, PackOutputLocation.resolve(source, null));
        assertEquals(expected, PackOutputLocation.resolve(source, ""));
        assertEquals(expected, PackOutputLocation.resolve(source, " \t"));
        assertEquals(expected, PackOutputLocation.resolve(source, "plugins/Oraxen/pack/pack.zip"));
    }

    @Test
    void rejectsOutputsThatWouldBecomePackInputs() throws Exception {
        Path source = Files.createDirectory(directory.resolve("pack"));
        for (Path output : Set.of(source.resolve("uploads/export.zip"),
                source.resolve("assets/export.zip"), source.resolve("pack.mcmeta"))) {
            assertThrows(IOException.class, () -> PackOutputLocation.resolve(source.toFile(), output.toString()));
        }
    }

    @Test
    void rejectsNestedSymlinkToAnOutputOutsideSource() throws Exception {
        Path source = directory.resolve("pack");
        Files.createDirectories(source.resolve("assets"));
        Path target = Files.writeString(directory.resolve("export.zip"), "previous");
        Path alias = source.resolve("assets/export.zip");
        try {
            Files.createSymbolicLink(alias, target);
        } catch (IOException | UnsupportedOperationException e) {
            Assumptions.assumeTrue(false, "Symbolic links unavailable: " + e);
        }
        assertThrows(IOException.class, () -> PackOutputLocation.resolve(source.toFile(), alias.toString()));
    }

    @Test
    void resolvesAbsoluteAndServerRelativeZipPaths() throws Exception {
        File source = directory.resolve("pack").toFile();
        Path output = directory.resolve("uploads/site/custom.zip");
        File outputFile = PackOutputLocation.resolve(source, output.toString());
        assertEquals(output.toFile().getCanonicalFile(), outputFile);
        Path relative = Path.of("test-output/custom.zip");
        assertEquals(relative.toFile().getCanonicalFile(), PackOutputLocation.resolve(source, relative.toString()));
    }
}
