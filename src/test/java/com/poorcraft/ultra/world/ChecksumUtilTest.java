package com.poorcraft.ultra.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChecksumUtilTest {

    @Test
    void testCRC32Deterministic() {
        byte[] data = "The quick brown fox jumps over the lazy dog".getBytes();
        long checksum1 = ChecksumUtil.computeCRC32(data);
        long checksum2 = ChecksumUtil.computeCRC32(data);
        assertEquals(checksum1, checksum2);
    }

    @Test
    void testCRC32DifferentData() {
        byte[] data1 = "hello world".getBytes();
        byte[] data2 = "hello world!".getBytes();
        assertNotEquals(ChecksumUtil.computeCRC32(data1), ChecksumUtil.computeCRC32(data2));
    }

    @Test
    void testCRC32EmptyArray() {
        byte[] empty = new byte[0];
        assertEquals(0L, ChecksumUtil.computeCRC32(empty));
    }

    @Test
    void testCRC32KnownValue() {
        byte[] data = "123456789".getBytes();
        long checksum = ChecksumUtil.computeCRC32(data);
        assertEquals(0xCBF43926L, checksum);
    }

    @Test
    void testVerifyCRC32Success() {
        byte[] data = "checksum".getBytes();
        long checksum = ChecksumUtil.computeCRC32(data);
        assertTrue(ChecksumUtil.verifyCRC32(data, checksum));
    }

    @Test
    void testVerifyCRC32Failure() {
        byte[] data = "checksum".getBytes();
        long checksum = ChecksumUtil.computeCRC32(data);
        assertFalse(ChecksumUtil.verifyCRC32(data, checksum + 1));
    }

    @Test
    void testCRC32Subset() {
        byte[] data = new byte[1024];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i & 0xFF);
        }
        long subsetChecksum = ChecksumUtil.computeCRC32(data, 100, 200);
        byte[] subset = new byte[200];
        System.arraycopy(data, 100, subset, 0, 200);
        assertEquals(ChecksumUtil.computeCRC32(subset), subsetChecksum);
    }
}
