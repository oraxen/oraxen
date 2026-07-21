package io.th0rgal.oraxen.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoveDefaultsCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void keepsGlobalLanguageOverridesWhenRemovingLanguageDefaults() throws IOException {
        Path languageFolder = Files.createDirectories(tempDir.resolve("pack/lang"));
        Path globalLanguage = Files.writeString(languageFolder.resolve("global.json"), "{\"custom.key\":\"value\"}");
        Path defaultLanguage = Files.writeString(languageFolder.resolve("en_us.json"), "{\"default.key\":\"value\"}");
        AtomicInteger deletedFiles = new AtomicInteger();
        AtomicInteger failedFiles = new AtomicInteger();

        new RemoveDefaultsCommand().deletePath(languageFolder, true, Set.of(globalLanguage), deletedFiles, failedFiles);

        assertTrue(Files.exists(languageFolder));
        assertTrue(Files.exists(globalLanguage));
        assertEquals("{\"custom.key\":\"value\"}", Files.readString(globalLanguage));
        assertFalse(Files.exists(defaultLanguage));
        assertEquals(1, deletedFiles.get());
        assertEquals(0, failedFiles.get());
    }
}
