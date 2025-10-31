package com.poorcraft.ultra.world;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdException;
import com.poorcraft.ultra.shared.Logger;
import com.poorcraft.ultra.voxel.Chunk;
import com.poorcraft.ultra.voxel.ChunkPos;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.zip.CRC32;

/**
 * Utility class responsible for serializing and deserializing {@link Chunk} instances to a custom
 * binary format which is subsequently compressed with Zstandard.
 */
public final class ChunkSerializer {

    private static final Logger logger = Logger.getLogger(ChunkSerializer.class);

    private static final int MAGIC_NUMBER = 0x504F4F52; // "POOR"
    private static final int FORMAT_VERSION = 1;
    private static final int HEADER_SIZE = Integer.BYTES * 2;
    private static final int BLOCK_DATA_SIZE = Chunk.TOTAL_BLOCKS * Short.BYTES;
    private static final int CHECKSUM_SIZE = Integer.BYTES;
    private static final int UNCOMPRESSED_SIZE = HEADER_SIZE + BLOCK_DATA_SIZE + CHECKSUM_SIZE;
    private static final int DEFAULT_COMPRESSION_LEVEL = 3;
    private static volatile int compressionLevel = DEFAULT_COMPRESSION_LEVEL;

    private ChunkSerializer() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static byte[] serialize(Chunk chunk) {
        Objects.requireNonNull(chunk, "chunk must not be null");

        try {
            byte[] blockBytes = toByteArray(chunk);
            long checksum = calculateChecksum(blockBytes);

            ByteArrayOutputStream baos = new ByteArrayOutputStream(UNCOMPRESSED_SIZE);
            try (DataOutputStream dataOut = new DataOutputStream(baos)) {
                dataOut.writeInt(MAGIC_NUMBER);
                dataOut.writeInt(FORMAT_VERSION);
                dataOut.write(blockBytes);
                dataOut.writeInt((int) checksum);
            }

            byte[] uncompressed = baos.toByteArray();
            byte[] compressed = Zstd.compress(uncompressed, compressionLevel);
            logger.debug("Serialized chunk ({}, {}) to {} bytes (compressed from {} bytes)",
                    chunk.getPosition().x(), chunk.getPosition().z(),
                    compressed.length, uncompressed.length);
            return compressed;
        } catch (IOException | ZstdException e) {
            throw new RuntimeException("Failed to serialize chunk at " + chunk.getPosition(), e);
        }
    }

    public static Chunk deserialize(byte[] compressedData, ChunkPos position) {
        Objects.requireNonNull(compressedData, "compressedData must not be null");
        Objects.requireNonNull(position, "position must not be null");

        try {
            byte[] uncompressed = new byte[UNCOMPRESSED_SIZE];
            long size = Zstd.decompress(uncompressed, compressedData);
            if (Zstd.isError(size)) {
                throw new IOException("Zstd decompression error: " + Zstd.getErrorName(size));
            }
            if (size != UNCOMPRESSED_SIZE) {
                throw new IOException(
                        "Unexpected decompressed size. Expected " + UNCOMPRESSED_SIZE + " but was " + size);
            }

            try (DataInputStream dataIn = new DataInputStream(new ByteArrayInputStream(uncompressed))) {
                int magic = dataIn.readInt();
                if (magic != MAGIC_NUMBER) {
                    throw new IOException("Invalid chunk magic number: 0x" + Integer.toHexString(magic));
                }

                int version = dataIn.readInt();
                if (version > FORMAT_VERSION) {
                    throw new IOException("Unsupported chunk format version: " + version);
                }

                byte[] blockBytes = new byte[BLOCK_DATA_SIZE];
                dataIn.readFully(blockBytes);
                long expectedChecksum = dataIn.readInt() & 0xFFFFFFFFL;
                long actualChecksum = calculateChecksum(blockBytes);
                if (expectedChecksum != actualChecksum) {
                    throw new IOException("Chunk data corrupted: checksum mismatch");
                }

                short[] blocks = toShortArray(blockBytes);
                Chunk chunk = new Chunk(position);
                chunk.setBlocksArray(blocks);
                chunk.setDirty(false);
                logger.debug("Deserialized chunk ({}, {}) from {} bytes", position.x(), position.z(),
                        compressedData.length);
                return chunk;
            }
        } catch (IOException | ZstdException e) {
            throw new RuntimeException("Failed to deserialize chunk at " + position, e);
        }
    }

    public static void setCompressionLevel(int level) {
        if (level < 1 || level > 22) {
            throw new IllegalArgumentException("Compression level must be between 1 and 22");
        }
        compressionLevel = level;
        logger.info("Chunk serializer compression level set to {}", level);
    }

    public static int getCompressionLevel() {
        return compressionLevel;
    }

    private static byte[] toByteArray(Chunk chunk) {
        ByteBuffer buffer = ByteBuffer.allocate(BLOCK_DATA_SIZE).order(ByteOrder.BIG_ENDIAN);
        chunk.copyBlocksTo(buffer);
        return buffer.array();
    }

    private static short[] toShortArray(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        short[] blocks = new short[Chunk.TOTAL_BLOCKS];
        for (int i = 0; i < blocks.length; i++) {
            blocks[i] = buffer.getShort();
        }
        return blocks;
    }

    private static long calculateChecksum(byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        return crc32.getValue();
    }
}
