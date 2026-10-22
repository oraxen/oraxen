package io.th0rgal.oraxen.pack.generation;

import io.th0rgal.oraxen.utils.ZipUtils;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

class DuplicateWriterTest {
    @TempDir Path temporary;

    @Test
    void localMigrationMarksOnlySuccessfulMigrationAndNeverMigratesMarkerAgain() throws Exception {
        Path source = temporary.resolve("item.json");
        String original = "{\"overrides\":[{\"predicate\":{\"custom_model_data\":1},\"model\":\"custom\"}]}";
        Files.writeString(source, original);
        assertFalse(DuplicationHandler.migrateLocalDuplicate(source, () -> false));
        assertEquals(original, Files.readString(source));
        java.util.concurrent.atomic.AtomicInteger migrations = new java.util.concurrent.atomic.AtomicInteger();
        assertTrue(DuplicationHandler.migrateLocalDuplicate(source, () -> { migrations.incrementAndGet(); return true; }));
        String marked = Files.readString(source);
        assertTrue(marked.startsWith("// This file was recognized as a duplicate"));
        assertTrue(marked.contains(original));
        assertFalse(DuplicationHandler.migrateLocalDuplicate(source, () -> { migrations.incrementAndGet(); return true; }));
        assertEquals(1, migrations.get());
        assertEquals(marked, Files.readString(source));
    }

    @Test
    void emptyLocalDuplicateDoesNotMigrateOrRewriteSource() throws Exception {
        Path source = temporary.resolve("empty.json");
        Files.writeString(source, "");
        assertFalse(DuplicationHandler.migrateLocalDuplicate(source, () -> { fail("Empty JSON cannot migrate"); return true; }));
        assertEquals("", Files.readString(source));
    }

    @Test
    void duplicateDoesNotAppendBytesToPreviousEntryAndFollowingEntryStillWorks() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (MockedStatic<Logs> logs = mockStatic(Logs.class);
             ZipOutputStream zip = new ZipOutputStream(bytes)) {
            ZipUtils.addToZip("same.json", new ByteArrayInputStream(new byte[0]), zip);
            ZipUtils.addToZip("same.json", new ByteArrayInputStream("wrong".getBytes(StandardCharsets.UTF_8)), zip);
            ZipUtils.addToZip("next.json", new ByteArrayInputStream("correct".getBytes(StandardCharsets.UTF_8)), zip);
        }
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            assertEquals("same.json", zip.getNextEntry().getName()); assertEquals(0, zip.readAllBytes().length);
            assertEquals("next.json", zip.getNextEntry().getName());
            assertEquals("correct", new String(zip.readAllBytes(), StandardCharsets.UTF_8)); assertNull(zip.getNextEntry());
        }
    }

    @Test
    void fontMergeKeepsDistinctProvidersWithSameCharacters() {
        List<VirtualFile> files = new ArrayList<>();
        files.add(font("{\"providers\":[{\"type\":\"bitmap\",\"file\":\"a.png\",\"chars\":[\"x\"],\"filter\":{\"uniform\":true}}]}"));
        files.add(font("{\"providers\":[{\"type\":\"bitmap\",\"file\":\"b.png\",\"chars\":[\"x\"],\"filter\":{\"uniform\":false}}]}"));
        try (MockedStatic<Logs> logs = mockStatic(Logs.class)) { DuplicationHandler.mergeFontFiles(files); }
        assertEquals(1, files.size());
        assertEquals(2, files.getFirst().toJsonObject().getAsJsonArray("providers").size());
        assertEquals("a.png", files.getFirst().toJsonObject().getAsJsonArray("providers").get(0).getAsJsonObject().get("file").getAsString());
    }

    private static VirtualFile font(String json) {
        return new VirtualFile("assets/test/font", "default.json", new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    }
}
