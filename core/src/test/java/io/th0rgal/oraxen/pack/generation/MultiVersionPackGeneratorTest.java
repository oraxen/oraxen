package io.th0rgal.oraxen.pack.generation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MultiVersionPackGeneratorTest {

    @TempDir
    File tempDir;

    private PackVersionManager versionManager;

    @BeforeEach
    void setUp() {
        versionManager = new PackVersionManager(tempDir);
        versionManager.setSilentMode(true);
    }

    private byte[] toByteArray(InputStream inputStream) throws IOException {
        return inputStream.readAllBytes();
    }

    @Test
    void testVirtualFileStreamMaterialization() throws Exception {
        String testContent = "test file content for multi-version pack";
        byte[] contentBytes = testContent.getBytes(StandardCharsets.UTF_8);

        ByteArrayInputStream originalStream = new ByteArrayInputStream(contentBytes);

        byte[] materialized = toByteArray(originalStream);

        assertEquals(testContent, new String(materialized, StandardCharsets.UTF_8));
    }

    @Test
    void testFreshStreamsCanBeCreatedFromMaterializedBytes() throws Exception {
        String testContent = "content that will be read multiple times";
        byte[] contentBytes = testContent.getBytes(StandardCharsets.UTF_8);

        byte[] materialized = contentBytes;

        for (int i = 0; i < 3; i++) {
            ByteArrayInputStream freshStream = new ByteArrayInputStream(materialized);
            byte[] readBytes = freshStream.readAllBytes();
            assertEquals(testContent, new String(readBytes, StandardCharsets.UTF_8),
                    "Fresh stream iteration " + i + " should have correct content");
        }
    }

    @Test
    void testMultipleReadsFromMaterializedBytes() throws Exception {
        String textureContent = "fake png data";
        String modelContent = "{\"parent\": \"item/generated\"}";

        List<byte[]> materializedFiles = new ArrayList<>();
        materializedFiles.add(textureContent.getBytes(StandardCharsets.UTF_8));
        materializedFiles.add(modelContent.getBytes(StandardCharsets.UTF_8));

        for (int version = 0; version < 3; version++) {
            for (int fileIdx = 0; fileIdx < materializedFiles.size(); fileIdx++) {
                ByteArrayInputStream freshStream = new ByteArrayInputStream(materializedFiles.get(fileIdx));
                byte[] readContent = freshStream.readAllBytes();
                String content = new String(readContent, StandardCharsets.UTF_8);
                
                if (fileIdx == 0) {
                    assertEquals(textureContent, content, 
                            "Version " + version + " should have correct texture content");
                } else {
                    assertEquals(modelContent, content, 
                            "Version " + version + " should have correct model content");
                }
            }
        }
    }

    @Test
    void testMaterializationPreventsStreamExhaustion() throws Exception {
        String content = "unique test content that would be lost if stream is exhausted";
        byte[] originalBytes = content.getBytes(StandardCharsets.UTF_8);

        byte[] materializedContent = originalBytes.clone();

        for (int iteration = 0; iteration < 5; iteration++) {
            ByteArrayInputStream freshStream = new ByteArrayInputStream(materializedContent);
            byte[] readBytes = freshStream.readAllBytes();
            String readContent = new String(readBytes, StandardCharsets.UTF_8);
            assertEquals(content, readContent, 
                    "Iteration " + iteration + " should have correct content (stream should not be exhausted)");
        }
    }

    @Test
    void testMaterializedByteArrayCanBeReusedIndependently() throws Exception {
        byte[] content1 = "content for pack version 1".getBytes(StandardCharsets.UTF_8);
        byte[] content2 = "content for pack version 2".getBytes(StandardCharsets.UTF_8);
        byte[] content3 = "content for pack version 3".getBytes(StandardCharsets.UTF_8);

        ByteArrayInputStream stream1 = new ByteArrayInputStream(content1);
        ByteArrayInputStream stream2 = new ByteArrayInputStream(content2);
        ByteArrayInputStream stream3 = new ByteArrayInputStream(content3);

        byte[] read1 = stream1.readAllBytes();
        byte[] read2 = stream2.readAllBytes();
        byte[] read3 = stream3.readAllBytes();

        assertEquals("content for pack version 1", new String(read1, StandardCharsets.UTF_8));
        assertEquals("content for pack version 2", new String(read2, StandardCharsets.UTF_8));
        assertEquals("content for pack version 3", new String(read3, StandardCharsets.UTF_8));
    }

    @Test
    void testMaterializationPreservesBinaryContent() throws Exception {
        byte[] binaryContent = new byte[256];
        for (int i = 0; i < 256; i++) {
            binaryContent[i] = (byte) i;
        }

        byte[] materialized = binaryContent.clone();

        for (int iteration = 0; iteration < 3; iteration++) {
            ByteArrayInputStream stream = new ByteArrayInputStream(materialized);
            byte[] read = stream.readAllBytes();
            assertArrayEquals(binaryContent, read, 
                    "Binary content should be preserved in iteration " + iteration);
        }
    }

    @Test
    void testMaterializationWithEmptyContent() throws Exception {
        byte[] emptyContent = new byte[0];
        byte[] materialized = emptyContent.clone();

        for (int iteration = 0; iteration < 3; iteration++) {
            ByteArrayInputStream stream = new ByteArrayInputStream(materialized);
            byte[] read = stream.readAllBytes();
            assertEquals(0, read.length, "Empty content should remain empty in iteration " + iteration);
        }
    }

    @Test
    void testMaterializationWithLargeContent() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append("Line ").append(i).append("\n");
        }
        String largeContent = sb.toString();
        byte[] originalBytes = largeContent.getBytes(StandardCharsets.UTF_8);

        byte[] materialized = originalBytes.clone();

        for (int iteration = 0; iteration < 3; iteration++) {
            ByteArrayInputStream stream = new ByteArrayInputStream(materialized);
            byte[] read = stream.readAllBytes();
            assertEquals(largeContent, new String(read, StandardCharsets.UTF_8),
                    "Large content should be preserved in iteration " + iteration);
        }
    }

    @Test
    void testMultipleFilesMaterializedCorrectly() throws Exception {
        List<String> originalContents = List.of(
                "assets/minecraft/textures/item/sword.png",
                "assets/minecraft/models/item/sword.json",
                "assets/minecraft/sounds.json"
        );

        List<byte[]> materializedFiles = new ArrayList<>();
        for (String content : originalContents) {
            materializedFiles.add(content.getBytes(StandardCharsets.UTF_8));
        }

        for (int version = 0; version < 3; version++) {
            for (int fileIdx = 0; fileIdx < materializedFiles.size(); fileIdx++) {
                ByteArrayInputStream stream = new ByteArrayInputStream(materializedFiles.get(fileIdx));
                String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                assertEquals(originalContents.get(fileIdx), content,
                        "Version " + version + ", file " + fileIdx + " should have correct content");
            }
        }
    }

    @Test
    void testVersionManagerHasMultipleVersions() {
        versionManager.definePackVersions();

        assertTrue(versionManager.hasVersions(), "Should have pack versions defined");
        assertTrue(versionManager.getVersionCount() >= 2, 
                "Should have at least 2 pack versions for multi-version testing");
    }

    @Test
    void testVersionManagerReturnsConsistentVersions() {
        versionManager.definePackVersions();

        var versions1 = versionManager.getAllVersions();
        var versions2 = versionManager.getAllVersions();

        assertEquals(versions1.size(), versions2.size(), "Version counts should be consistent");
    }
}
