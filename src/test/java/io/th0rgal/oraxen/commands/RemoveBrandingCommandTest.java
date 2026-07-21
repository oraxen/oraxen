package io.th0rgal.oraxen.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoveBrandingCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void createsSanitizedGlobalLanguageFileWhenLanguageFolderIsAbsent() throws IOException {
        Path languageFolder = tempDir.resolve("pack/lang");

        int updated = new RemoveBrandingCommand().updateExistingLangFiles(languageFolder);

        assertEquals(1, updated);
        assertTrue(Files.isDirectory(languageFolder));
        assertEquals("{}" + System.lineSeparator(), Files.readString(languageFolder.resolve("global.json")));
    }
}
