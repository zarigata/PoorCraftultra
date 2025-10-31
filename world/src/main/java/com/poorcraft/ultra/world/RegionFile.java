package com.poorcraft.ultra.world;

import com.poorcraft.ultra.shared.Logger;
import com.poorcraft.ultra.voxel.ChunkPos;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

/**
 * Manages storage of 32x32 chunk regions using a Minecraft-style region file layout (.mca).
 */
public final class RegionFile implements AutoCloseable {

    private static final Logger logger = Logger.getLogger(RegionFile.class);

    private static final int CHUNKS_PER_REGION = 32 * 32;
    private static final int OFFSET_TABLE_SIZE = CHUNKS_PER_REGION;
    private static final int SECTOR_SIZE = 4096;
    private static final int HEADER_SIZE = SECTOR_SIZE * 2; // offsets + reserved (timestamps)

    private final Path filePath;
    private final RegionPos regionPos;
    private final RandomAccessFile file;
    private final int[] offsets = new int[OFFSET_TABLE_SIZE];
    private final Object fileLock = new Object();

    private boolean closed;

    public RegionFile(Path filePath, RegionPos regionPos) throws IOException {
        this.filePath = Objects.requireNonNull(filePath, "filePath must not be null");
        this.regionPos = Objects.requireNonNull(regionPos, "regionPos must not be null");

        Files.createDirectories(filePath.getParent());
        this.file = new RandomAccessFile(filePath.toFile(), "rw");

        if (file.length() < HEADER_SIZE) {
            initializeHeader();
        }

        readOffsetTable();
        logger.debug("Region file opened: {}", filePath);
    }

    public boolean hasChunk(ChunkPos chunkPos) {
        Objects.requireNonNull(chunkPos, "chunkPos must not be null");
        ensureOpen();
        int offsetIndex = regionPos.getChunkOffset(chunkPos);
        synchronized (fileLock) {
            return offsets[offsetIndex] != 0;
        }
    }

    public byte[] readChunk(ChunkPos chunkPos) throws IOException {
        Objects.requireNonNull(chunkPos, "chunkPos must not be null");
        ensureOpen();
        int offsetIndex = regionPos.getChunkOffset(chunkPos);

        synchronized (fileLock) {
            int offsetEntry = offsets[offsetIndex];
            if (offsetEntry == 0) {
                return null;
            }

            int sectorNumber = offsetEntry >>> 8;
            int sectorCount = offsetEntry & 0xFF;
            long position = (long) sectorNumber * SECTOR_SIZE;

            file.seek(position);
            int length = file.readInt();
            if (length <= 0 || length > sectorCount * SECTOR_SIZE - Integer.BYTES) {
                throw new IOException("Invalid chunk length " + length + " for chunk " + chunkPos);
            }

            byte[] data = new byte[length];
            file.readFully(data);
            return data;
        }
    }

    public void writeChunk(ChunkPos chunkPos, byte[] compressedData) throws IOException {
        Objects.requireNonNull(chunkPos, "chunkPos must not be null");
        Objects.requireNonNull(compressedData, "compressedData must not be null");
        ensureOpen();

        if (!regionPos.containsChunk(chunkPos)) {
            throw new IllegalArgumentException("Chunk " + chunkPos + " does not belong to region " + regionPos);
        }

        int offsetIndex = regionPos.getChunkOffset(chunkPos);
        int sectorsNeeded = calculateSectorsNeeded(compressedData.length);

        synchronized (fileLock) {
            int sectorNumber = allocateSectors(sectorsNeeded);
            file.seek((long) sectorNumber * SECTOR_SIZE);
            file.writeInt(compressedData.length);
            file.write(compressedData);
            int padding = sectorsNeeded * SECTOR_SIZE - (Integer.BYTES + compressedData.length);
            if (padding > 0) {
                file.write(new byte[padding]);
            }

            int offsetEntry = (sectorNumber << 8) | (sectorsNeeded & 0xFF);
            offsets[offsetIndex] = offsetEntry;
            writeOffsetEntry(offsetIndex, offsetEntry);
            logger.debug("Chunk ({}, {}) written to region {} using {} sectors", chunkPos.x(), chunkPos.z(), regionPos,
                    sectorsNeeded);
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (fileLock) {
            if (closed) {
                return;
            }
            closed = true;
            file.close();
            logger.debug("Region file closed: {}", filePath);
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Region file already closed: " + filePath);
        }
    }

    private int allocateSectors(int sectorsNeeded) throws IOException {
        long length = file.length();
        long alignedLength = alignToSector(length);
        if (alignedLength != length) {
            file.setLength(alignedLength);
        }
        int nextSector = (int) (alignedLength / SECTOR_SIZE);
        if (nextSector < 2) {
            nextSector = 2; // Reserve first two sectors for header
        }
        long newLength = alignedLength + (long) sectorsNeeded * SECTOR_SIZE;
        file.setLength(newLength);
        return nextSector;
    }

    private static long alignToSector(long length) {
        long remainder = length % SECTOR_SIZE;
        return remainder == 0 ? length : length + (SECTOR_SIZE - remainder);
    }

    private static int calculateSectorsNeeded(int dataLength) {
        int total = dataLength + Integer.BYTES;
        return (total + SECTOR_SIZE - 1) / SECTOR_SIZE;
    }

    private void readOffsetTable() throws IOException {
        synchronized (fileLock) {
            file.seek(0);
            byte[] header = new byte[SECTOR_SIZE];
            try {
                file.readFully(header);
            } catch (EOFException e) {
                Arrays.fill(offsets, 0);
                return;
            }

            ByteBuffer buffer = ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN);
            for (int i = 0; i < OFFSET_TABLE_SIZE; i++) {
                offsets[i] = buffer.getInt();
            }
        }
    }

    private void writeOffsetEntry(int index, int value) throws IOException {
        long position = (long) index * Integer.BYTES;
        file.seek(position);
        file.writeInt(value);
    }

    private void initializeHeader() throws IOException {
        file.seek(0);
        byte[] zeroes = new byte[HEADER_SIZE];
        file.write(zeroes);
    }

}
