package io.th0rgal.oraxen.pack.upload.hosts;

import com.sun.net.httpserver.HttpServer;
import io.th0rgal.oraxen.utils.HashUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

class S3Test {
    @TempDir Path tempDir;
    private HttpServer server;
    private MemoryConfiguration config;
    private final List<Upload> uploads = new ArrayList<>();
    private volatile int status = 200;
    private MockedStatic<Logs> logs;

    @BeforeEach
    void startServer() throws IOException {
        logs = mockStatic(Logs.class);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body = exchange.getRequestBody().readAllBytes();
            synchronized (uploads) {
                uploads.add(new Upload(exchange.getRequestMethod(), exchange.getRequestURI(), body,
                        exchange.getRequestHeaders().getFirst("Authorization"),
                        exchange.getRequestHeaders().getFirst("Content-Type")));
            }
            exchange.getResponseHeaders().set("ETag", "\"not-a-sha1\"");
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
        config = new MemoryConfiguration();
        config.set("endpoint", "http://127.0.0.1:" + server.getAddress().getPort());
        config.set("access-key", "test-access");
        config.set("secret-key", "test-secret");
        config.set("bucket", "packs");
        config.set("region", "us-east-1");
        config.set("prefix", "/packs with spaces/café/");
        config.set("public-url", "https://cdn.example.com/");
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
        logs.close();
    }

    @Test
    void uploadsSignedZipAndPublishesLocalHashAndEncodedCdnUrl() throws Exception {
        Path file = pack("resourcepack.zip", "first pack");
        S3 provider = new S3(config);
        assertTrue(provider.uploadPack(file.toFile()));
        String hash = HashUtils.bytesToHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(file)));
        Upload upload = uploads.getFirst();
        assertEquals("PUT", upload.method());
        assertArrayEquals(Files.readAllBytes(file), upload.body());
        assertEquals("application/zip", upload.contentType());
        assertTrue(upload.authorization().startsWith("AWS4-HMAC-SHA256 Credential=test-access/"));
        assertTrue(upload.authorization().contains("/us-east-1/s3/aws4_request"));
        assertEquals("/packs/packs with spaces/café/pack-" + hash + ".zip", upload.uri().getPath());
        assertEquals("https://cdn.example.com/packs%20with%20spaces/caf%C3%A9/pack-" + hash + ".zip", provider.getPackURL());
        assertEquals(hash, provider.getOriginalSHA1());
        assertArrayEquals(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(file)), provider.getSHA1());
        assertEquals(UUID.nameUUIDFromBytes(provider.getSHA1()), provider.getPackUUID());
    }

    @Test
    void versionsAndChangedContentsHaveDistinctKeysAndIdenticalUploadsReuseKeys() throws IOException {
        Path file = pack("resourcepack.zip", "same content");
        S3 provider = new S3(config);
        assertTrue(provider.uploadPack(file.toFile(), "1.21.4"));
        String first = provider.getPackURL();
        assertTrue(provider.uploadPack(file.toFile(), "1.21.4"));
        assertEquals(first, provider.getPackURL());
        assertTrue(provider.uploadPack(file.toFile(), "1.21.5"));
        assertNotEquals(first, provider.getPackURL());
        Files.writeString(file, "changed content");
        assertTrue(provider.uploadPack(file.toFile(), "1.21.4"));
        assertNotEquals(first, provider.getPackURL());
    }

    @Test
    void privateBucketReturnsPresignedGetUrlAtConfiguredEndpoint() throws IOException {
        config.set("public-url", "");
        config.set("prefix", "");
        config.set("presigned-url-expiry", 3600);
        S3 provider = new S3(config);
        assertTrue(provider.uploadPack(pack("pack.zip", "private pack").toFile(), "1.21.4"));
        URI url = URI.create(provider.getPackURL());
        assertEquals("127.0.0.1", url.getHost());
        assertEquals(server.getAddress().getPort(), url.getPort());
        assertEquals("/packs/pack-1.21.4-" + provider.getOriginalSHA1() + ".zip", url.getPath());
        String query = URLDecoder.decode(url.getRawQuery(), StandardCharsets.UTF_8);
        assertTrue(query.contains("X-Amz-Algorithm=AWS4-HMAC-SHA256"));
        assertTrue(query.contains("X-Amz-Expires=3600"));
        assertTrue(query.contains("X-Amz-Signature="));
        assertFalse(provider.getPackURL().contains("test-secret"));
    }

    @Test
    void rejectedUploadPreservesLastSuccessfulState() throws IOException {
        S3 provider = new S3(config);
        assertTrue(provider.uploadPack(pack("old.zip", "old content").toFile()));
        String url = provider.getPackURL();
        String hash = provider.getOriginalSHA1();
        UUID uuid = provider.getPackUUID();
        status = 403;
        assertFalse(provider.uploadPack(pack("new.zip", "new content").toFile()));
        assertEquals(url, provider.getPackURL());
        assertEquals(hash, provider.getOriginalSHA1());
        assertEquals(uuid, provider.getPackUUID());
        logs.verify(() -> Logs.logError("S3 resource pack upload failed (HTTP 403)."));
    }

    @Test
    void invalidConfigurationFailsBeforeSendingOrPublishing() throws IOException {
        Path file = pack("pack.zip", "pack");
        config.set("secret-key", "");
        S3 provider = new S3(config);
        assertFalse(provider.uploadPack(file.toFile()));
        assertNull(provider.getPackURL());
        assertNull(provider.getSHA1());
        config.set("secret-key", "test-secret");
        config.set("endpoint", "https://test-access:test-secret@example.com");
        assertFalse(new S3(config).uploadPack(file.toFile()));
        config.set("endpoint", "http://127.0.0.1:" + server.getAddress().getPort());
        config.set("public-url", "https://cdn.example.com?token=test-secret");
        assertFalse(new S3(config).uploadPack(file.toFile()));
        config.set("public-url", "");
        for (long expiry : new long[]{0, -1, 604801}) {
            config.set("presigned-url-expiry", expiry);
            assertFalse(new S3(config).uploadPack(file.toFile()));
        }
        assertTrue(uploads.isEmpty());
    }

    @Test
    void missingFileDoesNotSendRequest() {
        assertFalse(new S3(config).uploadPack(tempDir.resolve("missing.zip").toFile()));
        assertTrue(uploads.isEmpty());
    }

    private Path pack(String name, String contents) throws IOException {
        return Files.writeString(tempDir.resolve(name), contents);
    }

    private record Upload(String method, URI uri, byte[] body, String authorization, String contentType) {}
}
