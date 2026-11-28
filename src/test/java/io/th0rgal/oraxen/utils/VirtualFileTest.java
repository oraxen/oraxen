package io.th0rgal.oraxen.utils;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VirtualFileTest {

    @Test
    void supplierBackedJsonParsesRepeatedlyAndClosesEveryStream() throws IOException {
        String content = "{\"providers\":[]}";
        List<ClosableStream> opened = new ArrayList<>();
        VirtualFile file = new VirtualFile("assets/minecraft/font", "default.json", () -> {
            ClosableStream stream = new ClosableStream(content.getBytes(StandardCharsets.UTF_8));
            opened.add(stream);
            return stream;
        });

        assertTrue(file.isJsonObject());
        JsonObject json = file.toJsonObject();

        assertNotNull(json);
        assertTrue(json.has("providers"));
        assertFalse(opened.isEmpty());
        assertTrue(opened.stream().allMatch(stream -> stream.closed));
        try (InputStream stream = file.getInputStream()) {
            assertEquals(content, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private static final class ClosableStream extends ByteArrayInputStream {
        private boolean closed;

        private ClosableStream(byte[] bytes) {
            super(bytes);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
