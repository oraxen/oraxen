package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.OraxenPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class MultipartBodyTest {

    @Test
    void snapshotsFilesBeforePublishing(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("pack.zip");
        Files.writeString(file, "original");

        Path cache = tempDir.resolve("plugins/Oraxen/.cache");
        OraxenPlugin plugin = mock(OraxenPlugin.class);
        when(plugin.getDataFolder()).thenReturn(cache.getParent().toFile());
        assertFalse(Files.exists(cache));

        try (var pluginAccess = mockStatic(OraxenPlugin.class)) {
            pluginAccess.when(OraxenPlugin::get).thenReturn(plugin);
            try (MultipartBody body = MultipartBody.create().addPart("pack", file.toFile())) {
                long contentLength = body.bodyPublisher().contentLength();
                Files.writeString(file, "replacement with a different length");

                var subscriber = HttpResponse.BodySubscribers.ofByteArray();
                body.bodyPublisher().subscribe(new Flow.Subscriber<ByteBuffer>() {
                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        subscriber.onSubscribe(subscription);
                    }

                    @Override
                    public void onNext(ByteBuffer buffer) {
                        subscriber.onNext(List.of(buffer));
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        subscriber.onError(throwable);
                    }

                    @Override
                    public void onComplete() {
                        subscriber.onComplete();
                    }
                });
                byte[] published = subscriber.getBody().toCompletableFuture().join();
                assertEquals(contentLength, published.length);
                String multipart = new String(published, StandardCharsets.UTF_8);
                assertTrue(multipart.contains("\r\n\r\noriginal\r\n"));
                assertFalse(multipart.contains("replacement"));
                try (var snapshots = Files.list(cache)) {
                    Path snapshot = snapshots.findFirst().orElseThrow();
                    assertEquals("original", Files.readString(snapshot));
                    assertTrue(snapshot.getFileName().toString().endsWith(".snapshot"));
                }
            }
        }
        assertCacheEmpty(cache);
    }

    @Test
    void removesSnapshotsWhenUploadFails(@TempDir Path tempDir) throws Exception {
        Path cache = tempDir.resolve(".cache");
        Path file = Files.writeString(tempDir.resolve("pack.zip"), "pack");

        assertThrows(IOException.class, () -> {
            try (MultipartBody body = new MultipartBody(cache)) {
                body.addPart("pack", file.toFile());
                throw new IOException("Upload failed");
            }
        });

        assertCacheEmpty(cache);
        assertEquals("pack", Files.readString(file));
    }

    @Test
    void removesIncompleteSnapshotWhenCopyFails(@TempDir Path tempDir) throws Exception {
        Path cache = tempDir.resolve(".cache");

        try (MultipartBody body = new MultipartBody(cache)) {
            assertThrows(UncheckedIOException.class,
                    () -> body.addPart("pack", tempDir.resolve("missing.zip").toFile()));
            assertCacheEmpty(cache);
        }
    }

    @Test
    void failsWhenCacheDirectoryCannotBeCreated(@TempDir Path tempDir) throws Exception {
        Path cache = Files.writeString(tempDir.resolve(".cache"), "existing file");
        Path file = Files.writeString(tempDir.resolve("pack.zip"), "pack");

        try (MultipartBody body = new MultipartBody(cache)) {
            assertThrows(UncheckedIOException.class, () -> body.addPart("pack", file.toFile()));
        }

        assertEquals("existing file", Files.readString(cache));
        assertEquals("pack", Files.readString(file));
    }

    private static void assertCacheEmpty(Path cache) throws IOException {
        try (var snapshots = Files.list(cache)) {
            assertEquals(0, snapshots.count());
        }
    }
}
