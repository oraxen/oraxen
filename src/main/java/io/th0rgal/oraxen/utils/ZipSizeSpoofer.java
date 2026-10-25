package io.th0rgal.oraxen.utils;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Discourages ordinary ZIP extraction while preserving Java ZipFile reads.
 * Java uses the central compressed size and DEFLATE end marker, ignoring the
 * local sizes and advertised uncompressed size. This is obfuscation, not encryption.
 */
final class ZipSizeSpoofer {
    private static final long zip64Marker = 0xffffffffL;

    private ZipSizeSpoofer() {
    }

    static void spoof(Path path) throws IOException {
        try (RandomAccessFile zip = new RandomAccessFile(path.toFile(), "rw")) {
            long end = findEnd(zip);
            long entries = unsignedShort(zip, end + 10);
            long directorySize = unsignedInt(zip, end + 12);
            long directoryOffset = unsignedInt(zip, end + 16);
            long directoryEnd = end;
            if (end >= 20 && unsignedInt(zip, end - 20) == 0x07064b50L) {
                long zip64End = positiveLong(zip, end - 12);
                if (zip64End > end - 76 || unsignedInt(zip, zip64End) != 0x06064b50L) {
                    throw new IOException("Invalid ZIP64 end record");
                }
                entries = positiveLong(zip, zip64End + 32);
                directorySize = positiveLong(zip, zip64End + 40);
                directoryOffset = positiveLong(zip, zip64End + 48);
                directoryEnd = zip64End;
            }
            if (directorySize > directoryEnd || directoryOffset != directoryEnd - directorySize
                    || entries > directorySize / 46) {
                throw new IOException("Invalid ZIP central directory bounds");
            }

            // Validate every header before changing anything. Walk records by their lengths,
            // never by scanning payload bytes, which may contain ZIP signatures themselves.
            List<EntryOffsets> offsets = new ArrayList<>();
            long position = directoryOffset;
            for (long index = 0; index < entries; index++) {
                if (position > directoryEnd - 46 || unsignedInt(zip, position) != 0x02014b50L
                        || unsignedShort(zip, position + 10) != 8) {
                    throw new IOException("Expected a DEFLATED ZIP central directory entry");
                }
                int nameLength = unsignedShort(zip, position + 28);
                int extraLength = unsignedShort(zip, position + 30);
                long next = position + 46 + nameLength + extraLength + unsignedShort(zip, position + 32);
                if (next > directoryEnd) throw new IOException("Invalid ZIP entry bounds");
                long size = unsignedInt(zip, position + 24);
                long compressedSize = unsignedInt(zip, position + 20);
                long localOffset = unsignedInt(zip, position + 42);
                long sizeOffset = position + 24;
                boolean zip64Size = size == zip64Marker;
                if (zip64Size || compressedSize == zip64Marker || localOffset == zip64Marker) {
                    long extra = findZip64Extra(zip, position + 46 + nameLength, extraLength);
                    int required = (zip64Size ? 8 : 0) + (compressedSize == zip64Marker ? 8 : 0)
                            + (localOffset == zip64Marker ? 8 : 0);
                    if (unsignedShort(zip, extra + 2) < required) {
                        throw new IOException("Truncated ZIP64 entry information");
                    }
                    if (zip64Size) sizeOffset = extra + 4;
                    if (localOffset == zip64Marker) {
                        localOffset = positiveLong(zip, extra + 4 + (zip64Size ? 8 : 0)
                                + (compressedSize == zip64Marker ? 8 : 0));
                    }
                }
                if (localOffset > directoryOffset - 30 || unsignedInt(zip, localOffset) != 0x04034b50L) {
                    throw new IOException("Invalid ZIP local header offset");
                }
                offsets.add(new EntryOffsets(position, localOffset, sizeOffset, zip64Size));
                position = next;
            }
            if (position != directoryEnd) throw new IOException("Unexpected data in ZIP central directory");

            for (EntryOffsets entry : offsets) {
                // Remove the descriptor flag so extractors trust the zero local sizes.
                // Keep descriptor bytes, central compressed sizes, CRCs and offsets intact.
                clearDescriptorFlag(zip, entry.local + 6);
                clearDescriptorFlag(zip, entry.central + 8);
                zip.seek(entry.local + 18);
                zip.writeLong(0L);
                zip.seek(entry.size);
                if (entry.zip64Size) zip.writeLong(0L);
                else zip.writeInt(0);
            }
        }
    }

    private static long findEnd(RandomAccessFile zip) throws IOException {
        long length = zip.length();
        for (long position = length - 22, minimum = Math.max(0, length - 22 - 65535);
                position >= minimum; position--) {
            if (unsignedInt(zip, position) == 0x06054b50L
                    && position + 22 + unsignedShort(zip, position + 20) == length) return position;
        }
        throw new IOException("Missing ZIP end record");
    }

    private static long findZip64Extra(RandomAccessFile zip, long start, int length) throws IOException {
        long end = start + length;
        for (long position = start; position <= end - 4;) {
            int size = unsignedShort(zip, position + 2);
            if (position + 4 + size > end) throw new IOException("Invalid ZIP extra field bounds");
            if (unsignedShort(zip, position) == 1) return position;
            position += 4 + size;
        }
        throw new IOException("Missing ZIP64 entry information");
    }

    private static void clearDescriptorFlag(RandomAccessFile zip, long position) throws IOException {
        int flags = unsignedShort(zip, position) & ~8;
        zip.seek(position);
        zip.writeShort(Short.reverseBytes((short) flags));
    }

    private static int unsignedShort(RandomAccessFile zip, long position) throws IOException {
        zip.seek(position);
        return Short.toUnsignedInt(Short.reverseBytes(zip.readShort()));
    }

    private static long unsignedInt(RandomAccessFile zip, long position) throws IOException {
        zip.seek(position);
        return Integer.toUnsignedLong(Integer.reverseBytes(zip.readInt()));
    }

    private static long positiveLong(RandomAccessFile zip, long position) throws IOException {
        zip.seek(position);
        long value = Long.reverseBytes(zip.readLong());
        if (value < 0) throw new IOException("Unsupported ZIP64 value");
        return value;
    }

    private record EntryOffsets(long central, long local, long size, boolean zip64Size) {
    }
}
