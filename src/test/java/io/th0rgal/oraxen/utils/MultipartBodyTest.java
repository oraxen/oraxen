package io.th0rgal.oraxen.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MultipartBodyTest {

    @Test
    void snapshotsFilesBeforePublishing(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("pack.zip");
        Files.writeString(file, "original");

        try (MultipartBody body = MultipartBody.create().addPart("pack", file.toFile())) {
            long contentLength = body.bodyPublisher().contentLength();
            Files.writeString(file, "replacement with a different length");

            assertEquals(contentLength, body.bodyPublisher().contentLength());
        }
    }
}
