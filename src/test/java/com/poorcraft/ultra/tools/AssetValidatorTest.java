package com.poorcraft.ultra.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AssetValidator.
 */
class AssetValidatorTest {
    
    @Test
    void testValidateSuccess(@TempDir Path tempDir) throws IOException {
        // Create valid manifest
        String manifest = """
            {
              "version": "1.0",
              "generated": "2025-01-15T10:30:00Z",
              "seed": 42,
              "assets": [
                {
                  "name": "wood_oak",
                  "category": "blocks",
                  "path": "blocks/wood_oak.png",
                  "width": 64,
                  "height": 64,
                  "sha256": "%s"
                },
                {
                  "name": "player_base",
                  "category": "skins",
                  "path": "skins/player_base.png",
                  "width": 256,
                  "height": 256,
                  "sha256": "%s"
                }
              ]
            }
            """;
        
        // Create directories
        Path blocksDir = tempDir.resolve("blocks");
        Path skinsDir = tempDir.resolve("skins");
        Files.createDirectories(blocksDir);
        Files.createDirectories(skinsDir);
        
        // Create test images
        Path blockImage = blocksDir.resolve("wood_oak.png");
        Path skinImage = skinsDir.resolve("player_base.png");
        
        createTestImage(blockImage, 64, 64);
        createTestImage(skinImage, 256, 256);
        
        // Compute hashes
        AssetValidator validator = new AssetValidator();
        String blockHash = computeHash(blockImage);
        String skinHash = computeHash(skinImage);
        
        // Write manifest with correct hashes
        Files.writeString(tempDir.resolve("manifest.json"), 
                         String.format(manifest, blockHash, skinHash));
        
        // Validate
        ValidationResult result = validator.validate(tempDir);
        
        assertTrue(result.valid());
        assertTrue(result.errors().isEmpty());
    }
    
    @Test
    void testValidateMissingManifest(@TempDir Path tempDir) {
        AssetValidator validator = new AssetValidator();
        ValidationResult result = validator.validate(tempDir);
        
        assertFalse(result.valid());
        assertFalse(result.errors().isEmpty());
        assertTrue(result.errors().get(0).contains("Manifest not found"));
    }
    
    @Test
    void testValidateWrongDimensions(@TempDir Path tempDir) throws IOException {
        // Create manifest expecting 64×64
        String manifest = """
            {
              "version": "1.0",
              "generated": "2025-01-15T10:30:00Z",
              "seed": 42,
              "assets": [
                {
                  "name": "wood_oak",
                  "category": "blocks",
                  "path": "blocks/wood_oak.png",
                  "width": 64,
                  "height": 64,
                  "sha256": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
                }
              ]
            }
            """;
        
        Files.writeString(tempDir.resolve("manifest.json"), manifest);
        
        // Create directory and image with wrong dimensions (128×128)
        Path blocksDir = tempDir.resolve("blocks");
        Files.createDirectories(blocksDir);
        createTestImage(blocksDir.resolve("wood_oak.png"), 128, 128);
        
        // Validate
        AssetValidator validator = new AssetValidator();
        ValidationResult result = validator.validate(tempDir);
        
        assertFalse(result.valid());
        assertTrue(result.errors().stream()
                  .anyMatch(err -> err.contains("expected 64x64, got 128x128")));
    }
    
    @Test
    void testValidateMissingFile(@TempDir Path tempDir) throws IOException {
        // Create manifest referencing non-existent file
        String manifest = """
            {
              "version": "1.0",
              "generated": "2025-01-15T10:30:00Z",
              "seed": 42,
              "assets": [
                {
                  "name": "wood_oak",
                  "category": "blocks",
                  "path": "blocks/wood_oak.png",
                  "width": 64,
                  "height": 64,
                  "sha256": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
                }
              ]
            }
            """;
        
        Files.writeString(tempDir.resolve("manifest.json"), manifest);
        
        // Don't create the file
        
        // Validate
        AssetValidator validator = new AssetValidator();
        ValidationResult result = validator.validate(tempDir);
        
        assertFalse(result.valid());
        assertTrue(result.errors().stream()
                  .anyMatch(err -> err.contains("file not found")));
    }
    
    @Test
    void testValidateHashMismatch(@TempDir Path tempDir) throws IOException {
        // Create manifest with wrong hash
        String manifest = """
            {
              "version": "1.0",
              "generated": "2025-01-15T10:30:00Z",
              "seed": 42,
              "assets": [
                {
                  "name": "wood_oak",
                  "category": "blocks",
                  "path": "blocks/wood_oak.png",
                  "width": 64,
                  "height": 64,
                  "sha256": "0000000000000000000000000000000000000000000000000000000000000000"
                }
              ]
            }
            """;
        
        Files.writeString(tempDir.resolve("manifest.json"), manifest);
        
        // Create directory and image
        Path blocksDir = tempDir.resolve("blocks");
        Files.createDirectories(blocksDir);
        createTestImage(blocksDir.resolve("wood_oak.png"), 64, 64);
        
        // Validate
        AssetValidator validator = new AssetValidator();
        ValidationResult result = validator.validate(tempDir);
        
        // Hash mismatch is a warning, not an error
        assertTrue(result.valid());
        assertFalse(result.warnings().isEmpty());
        assertTrue(result.warnings().stream()
                  .anyMatch(warn -> warn.contains("hash mismatch")));
    }
    
    @Test
    void testValidateSingle() throws IOException {
        // Create temporary image
        Path tempImage = Files.createTempFile("test", ".png");
        createTestImage(tempImage, 64, 64);
        
        AssetValidator validator = new AssetValidator();
        
        // Valid dimensions
        String error = validator.validateSingle(tempImage, 64, 64);
        assertNull(error);
        
        // Wrong dimensions
        error = validator.validateSingle(tempImage, 128, 128);
        assertNotNull(error);
        assertTrue(error.contains("Expected 128x128, got 64x64"));
        
        // Clean up
        Files.deleteIfExists(tempImage);
    }
    
    // Helper methods
    
    private void createTestImage(Path path, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        // Fill with solid color
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, 0xFF0000); // Red
            }
        }
        ImageIO.write(image, "PNG", path.toFile());
    }
    
    private String computeHash(Path path) throws IOException {
        java.security.MessageDigest digest;
        try {
            digest = java.security.MessageDigest.getInstance("SHA-256");
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        
        byte[] fileBytes = Files.readAllBytes(path);
        byte[] hashBytes = digest.digest(fileBytes);
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        return hexString.toString();
    }
}
