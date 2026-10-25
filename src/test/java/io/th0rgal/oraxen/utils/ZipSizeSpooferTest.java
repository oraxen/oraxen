package io.th0rgal.oraxen.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ZipSizeSpooferTest {
    @TempDir
    Path directory;

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 9})
    void keepsEveryAssetReadableWithoutAddingBytesAtEachCompressionLevel(int compression) throws Exception {
        byte[] binary = new byte[32768];
        new Random(1234).nextBytes(binary);
        Map<String, byte[]> assets = new LinkedHashMap<>();
        assets.put("pack.mcmeta", "{\"pack\":{\"pack_format\":15,\"description\":\"Protection\"}}".getBytes(StandardCharsets.UTF_8));
        assets.put("assets/oraxen/textures/test.png", binary);
        assets.put("overlays/new/assets/oraxen/lang/日本語.json", "{\"example\":\"日本語\"}".getBytes(StandardCharsets.UTF_8));
        assets.put("empty", new byte[0]);
        String comment = "Pack comment with false signatures PK\u0005\u0006 and PK\u0001\u0002";
        Path pack = write(assets, compression, comment);
        byte[] original = Files.readAllBytes(pack);

        ZipSizeSpoofer.spoof(pack);

        assertEquals(original.length, Files.size(pack));
        assertContents(pack, assets);
        try (ZipFile zip = new ZipFile(pack.toFile())) {
            assertEquals(comment, zip.getComment());
            for (ZipEntry entry : zip.stream().toList()) assertEquals(0, entry.getSize());
        }
        // A sequential extractor trusts the spoofed local sizes and cannot read the payload.
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(pack))) {
            assertNotNull(zip.getNextEntry());
            assertThrows(IOException.class, zip::readAllBytes);
        }
        byte[] once = Files.readAllBytes(pack);
        ZipSizeSpoofer.spoof(pack);
        assertArrayEquals(once, Files.readAllBytes(pack));
    }

    @Test
    void acceptsMaximumLengthZipComment() throws Exception {
        String comment = "x".repeat(65535);
        Map<String, byte[]> assets = Map.of("pack.mcmeta", new byte[] {1, 2, 3});
        Path pack = write(assets, 9, comment);
        ZipSizeSpoofer.spoof(pack);
        assertContents(pack, assets);
        try (ZipFile zip = new ZipFile(pack.toFile())) {
            assertEquals(comment, zip.getComment());
        }
    }

    @Test
    void supportsZip64EntryCount() throws Exception {
        Path pack = directory.resolve("many-entries.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(pack))) {
            for (int i = 0; i < 65536; i++) {
                zip.putNextEntry(new ZipEntry("assets/oraxen/" + i));
                zip.write(i & 255);
                zip.closeEntry();
            }
        }
        long length = Files.size(pack);
        ZipSizeSpoofer.spoof(pack);
        assertEquals(length, Files.size(pack));
        try (ZipFile zip = new ZipFile(pack.toFile())) {
            assertEquals(65536, zip.size());
            for (int i : new int[] {0, 32768, 65535}) {
                try (var input = zip.getInputStream(zip.getEntry("assets/oraxen/" + i))) {
                    assertArrayEquals(new byte[] {(byte) i}, input.readAllBytes());
                }
            }
        }
    }

    @Test
    void preservesZip64ExtraFieldOrderForSizesAndLocalOffset() throws Exception {
        Map<String, byte[]> assets = Map.of("pack.mcmeta", "ZIP64 content".getBytes(StandardCharsets.UTF_8));
        Path pack = write(assets, 9, "");
        byte[] original = Files.readAllBytes(pack);
        ByteBuffer source = ByteBuffer.wrap(original).order(ByteOrder.LITTLE_ENDIAN);
        int end = original.length - 22;
        int central = source.getInt(end + 16);
        int extra = central + 46 + Short.toUnsignedInt(source.getShort(central + 28));
        byte[] extended = new byte[original.length + 28];
        System.arraycopy(original, 0, extended, 0, extra);
        System.arraycopy(original, extra, extended, extra + 28, original.length - extra);
        ByteBuffer target = ByteBuffer.wrap(extended).order(ByteOrder.LITTLE_ENDIAN);
        target.putShort(extra, (short) 1).putShort(extra + 2, (short) 24);
        target.putLong(extra + 4, Integer.toUnsignedLong(source.getInt(central + 24)));
        target.putLong(extra + 12, Integer.toUnsignedLong(source.getInt(central + 20)));
        target.putLong(extra + 20, Integer.toUnsignedLong(source.getInt(central + 42)));
        target.putInt(central + 20, -1).putInt(central + 24, -1).putInt(central + 42, -1);
        target.putShort(central + 30, (short) (source.getShort(central + 30) + 28));
        target.putInt(end + 28 + 12, source.getInt(end + 12) + 28);
        Files.write(pack, extended);
        assertContents(pack, assets);

        ZipSizeSpoofer.spoof(pack);

        assertContents(pack, assets);
        assertEquals(-1, ByteBuffer.wrap(Files.readAllBytes(pack)).order(ByteOrder.LITTLE_ENDIAN).getInt(central + 24));
        try (ZipFile zip = new ZipFile(pack.toFile())) {
            assertEquals(0, zip.getEntry("pack.mcmeta").getSize());
        }
    }

    @Test
    void rejectsInvalidLaterHeaderBeforeChangingAnyEarlierHeader() throws Exception {
        Path pack = write(Map.of("one", new byte[] {1}, "two", new byte[] {2}), 9, "");
        byte[] bytes = Files.readAllBytes(pack);
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int central = buffer.getInt(bytes.length - 6);
        int second = central + 46 + Short.toUnsignedInt(buffer.getShort(central + 28))
                + Short.toUnsignedInt(buffer.getShort(central + 30));
        buffer.putInt(second, 0);
        Files.write(pack, bytes);
        assertThrows(IOException.class, () -> ZipSizeSpoofer.spoof(pack));
        assertArrayEquals(bytes, Files.readAllBytes(pack));
    }

    private Path write(Map<String, byte[]> assets, int compression, String comment) throws IOException {
        Path pack = directory.resolve("pack.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(pack))) {
            zip.setLevel(compression);
            zip.setComment(comment);
            for (var asset : assets.entrySet()) {
                zip.putNextEntry(new ZipEntry(asset.getKey()));
                zip.write(asset.getValue());
                zip.closeEntry();
            }
        }
        return pack;
    }

    private static void assertContents(Path pack, Map<String, byte[]> assets) throws IOException {
        try (ZipFile zip = new ZipFile(pack.toFile())) {
            assertEquals(assets.size(), zip.size());
            for (var asset : assets.entrySet()) {
                ZipEntry entry = zip.getEntry(asset.getKey());
                assertNotNull(entry);
                byte[] content;
                try (var input = zip.getInputStream(entry)) {
                    content = input.readAllBytes();
                }
                assertArrayEquals(asset.getValue(), content, asset.getKey());
                CRC32 crc = new CRC32();
                crc.update(content);
                assertEquals(crc.getValue(), entry.getCrc());
            }
        }
    }
}
