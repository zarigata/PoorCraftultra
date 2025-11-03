package com.poorcraft.ultra.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Runtime asset validator for Poorcraft Ultra.
 * Validates assets at application startup and displays status.
 */
public class AssetValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(AssetValidator.class);
    
    public enum ValidationStatus {
        OK,
        FAILED,
        NOT_RUN
    }
    
    private static ValidationStatus status = ValidationStatus.NOT_RUN;
    private static String validationMessage = "";
    
    private static final int BLOCK_SIZE = 64;
    private static final int SKIN_SIZE = 256;
    private static final int ITEM_SIZE = 64;
    
    /**
     * Validate all game assets.
     * 
     * @throws AssetValidationException if validation fails
     */
    public static void validateAll() {
        logger.info("Starting asset validation...");
        
        try {
            // Validate blocks
            Path blocksManifest = Paths.get("assets/blocks/manifest.json");
            if (Files.exists(blocksManifest)) {
                int blockCount = validateManifest(blocksManifest, BLOCK_SIZE, BLOCK_SIZE);
                logger.info("Validated {} block textures", blockCount);
            } else {
                throw new AssetValidationException("assets/blocks/manifest.json",
                    AssetValidationException.ValidationType.FILE_NOT_FOUND,
                    "Manifest file not found");
            }
            
            // Validate skins
            Path skinsManifest = Paths.get("assets/skins/manifest.json");
            if (Files.exists(skinsManifest)) {
                int skinCount = validateManifest(skinsManifest, SKIN_SIZE, SKIN_SIZE);
                logger.info("Validated {} skin textures", skinCount);
            } else {
                throw new AssetValidationException("assets/skins/manifest.json",
                    AssetValidationException.ValidationType.FILE_NOT_FOUND,
                    "Manifest file not found");
            }
            
            // Validate items
            Path itemsManifest = Paths.get("assets/items/manifest.json");
            if (Files.exists(itemsManifest)) {
                int itemCount = validateManifest(itemsManifest, ITEM_SIZE, ITEM_SIZE);
                logger.info("Validated {} item icons", itemCount);
            } else {
                throw new AssetValidationException("assets/items/manifest.json",
                    AssetValidationException.ValidationType.FILE_NOT_FOUND,
                    "Manifest file not found");
            }
            
            status = ValidationStatus.OK;
            validationMessage = "All assets validated successfully";
            logger.info("Asset validation passed: all textures valid");
            
        } catch (AssetValidationException e) {
            status = ValidationStatus.FAILED;
            validationMessage = e.getMessage();
            logger.error("Asset validation failed", e);
            throw e;
        } catch (Exception e) {
            status = ValidationStatus.FAILED;
            validationMessage = "Unexpected error: " + e.getMessage();
            logger.error("Unexpected error during asset validation", e);
            throw new AssetValidationException("unknown",
                AssetValidationException.ValidationType.INVALID_FORMAT,
                e.getMessage());
        }
    }
    
    /**
     * Validate assets from a manifest file.
     * 
     * @param manifestPath Path to manifest.json
     * @param expectedWidth Expected texture width
     * @param expectedHeight Expected texture height
     * @return Number of textures validated
     * @throws AssetValidationException if validation fails
     */
    public static int validateManifest(Path manifestPath, int expectedWidth, int expectedHeight) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode manifest = mapper.readTree(manifestPath.toFile());
            
            JsonNode textures = manifest.get("textures");
            if (textures == null || !textures.isArray()) {
                throw new AssetValidationException(manifestPath.toString(),
                    AssetValidationException.ValidationType.INVALID_FORMAT,
                    "Invalid manifest format: missing textures array");
            }
            
            Path assetDir = manifestPath.getParent();
            int count = 0;
            
            for (JsonNode texture : textures) {
                String name = texture.get("name").asText();
                int width = texture.get("width").asInt();
                int height = texture.get("height").asInt();
                String expectedHash = texture.get("hash").asText();
                
                Path texturePath = assetDir.resolve(name);
                
                // Check file exists
                if (!Files.exists(texturePath)) {
                    throw new AssetValidationException(texturePath.toString(),
                        AssetValidationException.ValidationType.FILE_NOT_FOUND,
                        "Texture file not found");
                }
                
                // Validate dimensions
                BufferedImage image = ImageIO.read(texturePath.toFile());
                if (image == null) {
                    throw new AssetValidationException(texturePath.toString(),
                        AssetValidationException.ValidationType.INVALID_FORMAT,
                        "Failed to load image");
                }
                
                if (image.getWidth() != expectedWidth || image.getHeight() != expectedHeight) {
                    throw new AssetValidationException(texturePath.toString(),
                        expectedWidth, expectedHeight,
                        image.getWidth(), image.getHeight());
                }
                
                // Validate hash
                String actualHash = computeHash(texturePath);
                if (!actualHash.equals(expectedHash)) {
                    throw new AssetValidationException(texturePath.toString(),
                        expectedHash, actualHash);
                }
                
                count++;
            }
            
            return count;
            
        } catch (IOException e) {
            throw new AssetValidationException(manifestPath.toString(),
                AssetValidationException.ValidationType.INVALID_FORMAT,
                "Failed to read manifest: " + e.getMessage());
        }
    }
    
    /**
     * Compute SHA-256 hash of a file.
     * 
     * @param path File path
     * @return Hex string of hash
     */
    private static String computeHash(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            try (InputStream is = Files.newInputStream(path)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            
            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException("Failed to compute hash", e);
        }
    }
    
    /**
     * Get current validation status.
     * 
     * @return Validation status
     */
    public static ValidationStatus getValidationStatus() {
        return status;
    }
    
    /**
     * Get validation message.
     * 
     * @return Human-readable status message
     */
    public static String getValidationMessage() {
        return validationMessage;
    }
    
    /**
     * Main method for running validator standalone (called by Gradle).
     */
    public static void main(String[] args) {
        try {
            validateAll();
            System.out.println("Asset validation passed!");
            System.exit(0);
        } catch (AssetValidationException e) {
            System.err.println("Asset validation failed:");
            System.err.println(e.getDetailedMessage());
            System.exit(1);
        }
    }
}
