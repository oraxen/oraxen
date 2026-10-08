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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

class SelfHostTest {
    @TempDir Path tempDir;
    private SelfHost provider;
    private MockedStatic<Logs> logs;

    @BeforeEach
    void setUp() {
        logs = mockStatic(Logs.class);
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("host", "127.0.0.1");
        config.set("port", 0);
        config.set("domain", "packs.example.test:25712");
        provider = new SelfHost(config);
    }

    @AfterEach
    void tearDown() throws Exception {
        try {
            var stopServer = SelfHost.class.getDeclaredMethod("stopServer");
            stopServer.setAccessible(true);
            stopServer.invoke(provider);
        } finally {
            logs.close();
        }
    }

    @Test
    void downloadsKeepPublishedHashWhileGeneratedZipIsRewritten() throws Exception {
        Path file = tempDir.resolve("pack.zip");
        byte[] published = zip("published pack");
        Files.write(file, published);
        assertTrue(provider.uploadPack(file.toFile()));
        byte[] hash = sha1(published);
        assertEquals("http://packs.example.test:25712/pack.zip", provider.getPackURL());
        assertArrayEquals(hash, provider.getSHA1());
        assertEquals(HashUtils.bytesToHex(hash), provider.getOriginalSHA1());
        assertEquals(UUID.nameUUIDFromBytes(hash), provider.getPackUUID());

        for (int attempt = 0; attempt < 3; attempt++) {
            Files.write(file, zip("regeneration " + attempt));
            byte[] downloaded = download();
            assertArrayEquals(hash, sha1(downloaded), "Downloaded SHA-1 must match the advertised SHA-1");
            assertArrayEquals(published, downloaded);
        }
        Files.delete(file);
        assertArrayEquals(published, download());
    }

    @Test
    void successfulUploadReplacesSnapshotAndFailedUploadPreservesIt() throws Exception {
        Path file = tempDir.resolve("pack.zip");
        Files.write(file, zip("first pack"));
        assertTrue(provider.uploadPack(file.toFile()));
        String firstHash = provider.getOriginalSHA1();

        byte[] replacement = zip("replacement pack");
        Files.write(file, replacement);
        assertTrue(provider.uploadPack(file.toFile()));
        assertNotEquals(firstHash, provider.getOriginalSHA1());
        byte[] hash = provider.getSHA1();
        UUID uuid = provider.getPackUUID();
        String url = provider.getPackURL();
        assertArrayEquals(sha1(replacement), hash);
        assertArrayEquals(replacement, download());

        assertFalse(provider.uploadPack(tempDir.resolve("missing.zip").toFile()));
        assertEquals(url, provider.getPackURL());
        assertArrayEquals(hash, provider.getSHA1());
        assertEquals(uuid, provider.getPackUUID());
        assertArrayEquals(replacement, download());
    }

    @Test
    void activeDownloadFinishesWithItsOriginalBytesDuringUpload() throws Exception {
        byte[] contents = new byte[8 * 1024 * 1024];
        new Random(0).nextBytes(contents);
        byte[] original = zip(contents);
        Path file = Files.write(tempDir.resolve("pack.zip"), original);
        assertTrue(provider.uploadPack(file.toFile()));

        HttpURLConnection connection = connection();
        try {
            assertEquals(200, connection.getResponseCode());
            try (InputStream input = connection.getInputStream()) {
                byte[] prefix = input.readNBytes(1024);
                byte[] replacement = zip("replacement");
                Files.write(file, replacement);
                assertTrue(provider.uploadPack(file.toFile()));
                ByteArrayOutputStream downloaded = new ByteArrayOutputStream();
                downloaded.write(prefix);
                input.transferTo(downloaded);
                assertArrayEquals(original, downloaded.toByteArray());
                assertArrayEquals(replacement, download());
            }
        } finally {
            connection.disconnect();
        }
    }

    private HttpURLConnection connection() throws Exception {
        var serverField = SelfHost.class.getDeclaredField("httpServer");
        serverField.setAccessible(true);
        HttpServer server = (HttpServer) serverField.get(provider);
        URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/pack.zip");
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        return connection;
    }

    private byte[] download() throws Exception {
        HttpURLConnection connection = connection();
        try {
            assertEquals(200, connection.getResponseCode());
            assertEquals("application/zip", connection.getContentType());
            try (InputStream input = connection.getInputStream()) {
                byte[] bytes = input.readAllBytes();
                assertEquals(bytes.length, connection.getContentLengthLong());
                return bytes;
            }
        } finally {
            connection.disconnect();
        }
    }

    private static byte[] zip(String contents) throws Exception {
        return zip(contents.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] zip(byte[] contents) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("pack.mcmeta"));
            zip.write(contents);
            zip.closeEntry();
        }
        return output.toByteArray();
    }

    private static byte[] sha1(byte[] bytes) throws Exception {
        return MessageDigest.getInstance("SHA-1").digest(bytes);
    }
}
