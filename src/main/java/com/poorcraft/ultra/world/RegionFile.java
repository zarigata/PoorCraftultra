package com.poorcraft.ultra.world;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public final class RegionFile {

    private static final int BLOCK_DATA_SIZE = 65_536;
    private static final int FILE_SIZE = RegionFileHeader.HEADER_SIZE_BYTES + BLOCK_DATA_SIZE;

    private RegionFile() {
    }

    public static void write(Path filePath, RegionFileHeader header, byte[] blockData) throws IOException {
        if (blockData.length != BLOCK_DATA_SIZE) {
            throw new IllegalArgumentException("Block data must be exactly " + BLOCK_DATA_SIZE + " bytes");
        }

        Path parentDir = filePath.getParent();
        Files.createDirectories(parentDir);

        ByteBuffer buffer = ByteBuffer.allocate(FILE_SIZE).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(header.magic());
        buffer.putInt(header.version());
        buffer.putInt(header.chunkX());
        buffer.putInt(header.chunkZ());
        buffer.putLong(header.timestamp());
        buffer.putInt(header.crc32());
        buffer.putInt(header.reserved());
        buffer.put(blockData);
        buffer.flip();

        Path tempFile = parentDir.resolve(filePath.getFileName().toString() + ".tmp");

        try (FileChannel channel = FileChannel.open(tempFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
            try {
                Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            if (Files.exists(tempFile)) {
                Files.delete(tempFile);
            }
        }
    }

    public static RegionFileData read(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("Region file not found: " + filePath);
        }

        long size = Files.size(filePath);
        if (size != FILE_SIZE) {
            throw new RegionFileException("Invalid file size for " + filePath + ": expected " + FILE_SIZE + " bytes but was " + size);
        }

        ByteBuffer buffer = ByteBuffer.allocate(FILE_SIZE).order(ByteOrder.BIG_ENDIAN);
        try (FileChannel channel = FileChannel.open(filePath, StandardOpenOption.READ)) {
            while (buffer.hasRemaining()) {
                int read = channel.read(buffer);
                if (read == -1) {
                    break;
                }
            }
        }
        buffer.flip();

        RegionFileHeader header = new RegionFileHeader(
                buffer.getInt(),
                buffer.getInt(),
                buffer.getInt(),
                buffer.getInt(),
                buffer.getLong(),
                buffer.getInt(),
                buffer.getInt()
        );

        if (!header.isValid()) {
            throw new RegionFileException("Invalid region file header for " + filePath + ": magic=" + header.magic() + ", version=" + header.version());
        }

        byte[] blockData = new byte[BLOCK_DATA_SIZE];
        buffer.get(blockData);

        if (!ChecksumUtil.verifyCRC32(blockData, Integer.toUnsignedLong(header.crc32()))) {
            throw new RegionFileException("Checksum mismatch for region file " + filePath);
        }

        return new RegionFileData(header, blockData);
    }

    public record RegionFileData(RegionFileHeader header, byte[] blockData) {
    }

    public static class RegionFileException extends IOException {
        public RegionFileException(String message) {
            super(message);
        }

        public RegionFileException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
