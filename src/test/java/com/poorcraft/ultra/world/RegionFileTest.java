package com.poorcraft.ultra.world;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class RegionFileTest {

    @Test
    void testWriteAndRead(@TempDir Path tempDir) throws IOException {
        byte[] blockData = createTestBlockData();
        long crc = ChecksumUtil.computeCRC32(blockData);
        RegionFileHeader header = RegionFileHeader.create(1, -2, (int) crc);
        Path file = tempDir.resolve("r.1.-2.dat");

        RegionFile.write(file, header, blockData);
        RegionFile.RegionFileData data = RegionFile.read(file);

        assertEquals(header.chunkX(), data.header().chunkX());
        assertEquals(header.chunkZ(), data.header().chunkZ());
        assertEquals(header.crc32(), data.header().crc32());
        assertArrayEquals(blockData, data.blockData());
    }

    @Test
    void testWriteAndReadWithUnsignedCrc32(@TempDir Path tempDir) throws IOException {
        byte[] blockData = createTestBlockData();
        long crc = ChecksumUtil.computeCRC32(blockData);
        assertTrue(crc > 0x7FFF_FFFFL, "Test setup must use CRC32 value greater than signed int max");
        assertTrue((int) crc < 0, "Stored CRC32 should be negative when interpreted as signed int");

        RegionFileHeader header = RegionFileHeader.create(4, -5, (int) crc);
        Path file = tempDir.resolve("r.4.-5.dat");

        RegionFile.write(file, header, blockData);
        RegionFile.RegionFileData data = RegionFile.read(file);

        assertEquals(header.crc32(), data.header().crc32());
        assertArrayEquals(blockData, data.blockData());
    }

    @Test
    void testReadNonexistentFile(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("missing.dat");
        assertThrows(FileNotFoundException.class, () -> RegionFile.read(missing));
    }

    @Test
    void testReadInvalidMagicNumber(@TempDir Path tempDir) throws IOException {
        byte[] blockData = createTestBlockData();
        long crc = ChecksumUtil.computeCRC32(blockData);
        ByteBuffer buffer = ByteBuffer.allocate(RegionFileHeader.HEADER_SIZE_BYTES + blockData.length)
                .order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(0x12345678); // invalid magic
        buffer.putInt(RegionFileHeader.CURRENT_VERSION);
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putLong(System.currentTimeMillis());
        buffer.putInt((int) crc);
        buffer.putInt(0);
        buffer.put(blockData);
        Path file = tempDir.resolve("invalid_magic.dat");
        Files.write(file, buffer.array());

        assertThrows(RegionFile.RegionFileException.class, () -> RegionFile.read(file));
    }

    @Test
    void testReadChecksumMismatch(@TempDir Path tempDir) throws IOException {
        byte[] blockData = createTestBlockData();
        long crc = ChecksumUtil.computeCRC32(blockData);
        RegionFileHeader header = RegionFileHeader.create(0, 0, (int) crc);
        Path file = tempDir.resolve("r.0.0.dat");
        RegionFile.write(file, header, blockData);

        byte[] bytes = Files.readAllBytes(file);
        int dataOffset = RegionFileHeader.HEADER_SIZE_BYTES + 10;
        bytes[dataOffset] ^= 0xFF;
        Files.write(file, bytes);

        assertThrows(RegionFile.RegionFileException.class, () -> RegionFile.read(file));
    }

    @Test
    void testReadWrongFileSize(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("wrong_size.dat");
        Files.write(file, new byte[1000]);
        assertThrows(RegionFile.RegionFileException.class, () -> RegionFile.read(file));
    }

    @Test
    void testWriteCreatesDirectories(@TempDir Path tempDir) throws IOException {
        byte[] blockData = createTestBlockData();
        long crc = ChecksumUtil.computeCRC32(blockData);
        RegionFileHeader header = RegionFileHeader.create(2, 3, (int) crc);
        Path file = tempDir.resolve("region/subdir/r.2.3.dat");

        RegionFile.write(file, header, blockData);

        assertTrue(Files.exists(file));
        assertTrue(Files.isDirectory(file.getParent()));
    }

    private static byte[] createTestBlockData() {
        byte[] data = new byte[65_536];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i & 0xFF);
        }
        return Arrays.copyOf(data, data.length);
    }
}
