package com.poorcraft.ultra.world;

public record RegionFileHeader(
        int magic,
        int version,
        int chunkX,
        int chunkZ,
        long timestamp,
        int crc32,
        int reserved) {

    public static final int MAGIC_NUMBER = 0x50435246; // "PCRF"
    public static final int CURRENT_VERSION = 1;
    public static final int HEADER_SIZE_BYTES = 32;

    public static RegionFileHeader create(int chunkX, int chunkZ, int crc32) {
        return new RegionFileHeader(
                MAGIC_NUMBER,
                CURRENT_VERSION,
                chunkX,
                chunkZ,
                System.currentTimeMillis(),
                crc32,
                0
        );
    }

    public boolean isValid() {
        return magic == MAGIC_NUMBER && version == CURRENT_VERSION;
    }
}
