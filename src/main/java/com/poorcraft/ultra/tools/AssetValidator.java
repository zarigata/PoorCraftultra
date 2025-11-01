package com.poorcraft.ultra.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Asset validation service.
 * 
 * Validates asset dimensions, file existence, and manifest integrity.
 */
public class AssetValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(AssetValidator.class);
    
    /**
     * Validate all assets in directory.
     * 
     * @param assetsRoot Root directory containing assets and manifest.json
     * @return ValidationResult with errors and warnings
     */
    public ValidationResult validate(Path assetsRoot) {
        logger.debug("Validating assets in: {}", assetsRoot);
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Check if manifest exists
        Path manifestPath = assetsRoot.resolve("manifest.json");
        if (!Files.exists(manifestPath)) {
            errors.add("Manifest not found: " + manifestPath);
            return ValidationResult.failure(errors);
        }
        
        // Parse manifest
        AssetManifest manifest;
        try {
            String manifestJson = Files.readString(manifestPath);
            manifest = AssetManifest.fromJson(manifestJson);
            logger.debug("Loaded manifest version {} with {} assets", 
                        manifest.version(), manifest.assets().size());
        } catch (IOException e) {
            errors.add("Failed to read manifest: " + e.getMessage());
            return ValidationResult.failure(errors);
        } catch (IllegalArgumentException e) {
            errors.add("Invalid manifest JSON: " + e.getMessage());
            return ValidationResult.failure(errors);
        }
        
        // Validate each asset
        for (AssetManifest.AssetEntry entry : manifest.assets()) {
            validateAssetEntry(assetsRoot, entry, errors, warnings);
        }
        
        // Return result
        if (!errors.isEmpty()) {
            return ValidationResult.failure(errors);
        } else if (!warnings.isEmpty()) {
            return ValidationResult.withWarnings(warnings);
        } else {
            return ValidationResult.success();
        }
    }
    
    /**
     * Validate a single asset entry.
     */
    private void validateAssetEntry(Path assetsRoot, AssetManifest.AssetEntry entry,
                                    List<String> errors, List<String> warnings) {
        Path assetPath = assetsRoot.resolve(entry.path());
        
        // Check file exists
        if (!Files.exists(assetPath)) {
            errors.add(entry.path() + ": file not found");
            return;
        }
        
        // Load and validate image
        BufferedImage image;
        try {
            image = ImageIO.read(assetPath.toFile());
            if (image == null) {
                errors.add(entry.path() + ": failed to load image (unsupported format?)");
                return;
            }
        } catch (IOException e) {
            errors.add(entry.path() + ": failed to read image - " + e.getMessage());
            return;
        }
        
        // Validate dimensions
        if (image.getWidth() != entry.width() || image.getHeight() != entry.height()) {
            errors.add(String.format("%s: expected %dx%d, got %dx%d",
                                   entry.path(), entry.width(), entry.height(),
                                   image.getWidth(), image.getHeight()));
        }
        
        // Validate category-specific rules
        String categoryError = validateCategoryRules(entry, image.getWidth(), image.getHeight());
        if (categoryError != null) {
            errors.add(entry.path() + ": " + categoryError);
        }
        
        // Optionally verify SHA-256 hash (warning only if mismatch)
        try {
            String actualHash = computeSha256(assetPath);
            if (!actualHash.equalsIgnoreCase(entry.sha256())) {
                warnings.add(entry.path() + ": hash mismatch (expected " + 
                           entry.sha256().substring(0, 8) + "..., got " + 
                           actualHash.substring(0, 8) + "...), may be outdated");
            }
        } catch (Exception e) {
            warnings.add(entry.path() + ": failed to compute hash - " + e.getMessage());
        }
    }
    
    /**
     * Validate category-specific dimension rules.
     * 
     * @return Error message if validation fails, null if valid
     */
    private String validateCategoryRules(AssetManifest.AssetEntry entry, int width, int height) {
        return switch (entry.category()) {
            case "blocks" -> {
                if (width != 64 || height != 64) {
                    yield "blocks must be 64×64";
                }
                yield null;
            }
            case "skins" -> {
                if (width != 256 || height != 256) {
                    yield "skins must be 256×256";
                }
                yield null;
            }
            case "items" -> {
                if (width != 64 || height != 64) {
                    yield "items must be 64×64";
                }
                yield null;
            }
            default -> "unknown category: " + entry.category();
        };
    }
    
    /**
     * Validate a single image file.
     * 
     * @param imageFile Path to image file
     * @param expectedWidth Expected width in pixels
     * @param expectedHeight Expected height in pixels
     * @return Error message if validation fails, null if valid
     */
    public String validateSingle(Path imageFile, int expectedWidth, int expectedHeight) {
        if (!Files.exists(imageFile)) {
            return "File not found";
        }
        
        try {
            BufferedImage image = ImageIO.read(imageFile.toFile());
            if (image == null) {
                return "Failed to load image";
            }
            
            if (image.getWidth() != expectedWidth || image.getHeight() != expectedHeight) {
                return String.format("Expected %dx%d, got %dx%d",
                                   expectedWidth, expectedHeight,
                                   image.getWidth(), image.getHeight());
            }
            
            return null; // Valid
        } catch (IOException e) {
            return "Failed to read image: " + e.getMessage();
        }
    }
    
    /**
     * Compute SHA-256 hash of file.
     */
    private String computeSha256(Path filePath) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        // Stream the file to avoid loading large assets entirely into memory.
        try (var inputStream = Files.newInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }

        byte[] hashBytes = digest.digest();

        // Convert to hex string
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
