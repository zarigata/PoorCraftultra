package com.poorcraft.ultra.tools;

/**
 * Custom exception for asset validation failures.
 */
public class AssetValidationException extends RuntimeException {
    
    public enum ValidationType {
        SIZE_MISMATCH,
        HASH_MISMATCH,
        FILE_NOT_FOUND,
        INVALID_FORMAT
    }
    
    private final String assetName;
    private final ValidationType validationType;
    private final int expectedWidth;
    private final int expectedHeight;
    private final int actualWidth;
    private final int actualHeight;
    private final String expectedHash;
    private final String actualHash;
    
    public AssetValidationException(String assetName, ValidationType type, String details) {
        super(buildMessage(assetName, type, details));
        this.assetName = assetName;
        this.validationType = type;
        this.expectedWidth = -1;
        this.expectedHeight = -1;
        this.actualWidth = -1;
        this.actualHeight = -1;
        this.expectedHash = null;
        this.actualHash = null;
    }
    
    public AssetValidationException(String assetName, int expectedWidth, int expectedHeight,
                                   int actualWidth, int actualHeight) {
        super(buildSizeMessage(assetName, expectedWidth, expectedHeight, actualWidth, actualHeight));
        this.assetName = assetName;
        this.validationType = ValidationType.SIZE_MISMATCH;
        this.expectedWidth = expectedWidth;
        this.expectedHeight = expectedHeight;
        this.actualWidth = actualWidth;
        this.actualHeight = actualHeight;
        this.expectedHash = null;
        this.actualHash = null;
    }
    
    public AssetValidationException(String assetName, String expectedHash, String actualHash) {
        super(buildHashMessage(assetName, expectedHash, actualHash));
        this.assetName = assetName;
        this.validationType = ValidationType.HASH_MISMATCH;
        this.expectedWidth = -1;
        this.expectedHeight = -1;
        this.actualWidth = -1;
        this.actualHeight = -1;
        this.expectedHash = expectedHash;
        this.actualHash = actualHash;
    }
    
    private static String buildMessage(String assetName, ValidationType type, String details) {
        return String.format("Asset validation failed: %s - %s: %s", assetName, type, details);
    }
    
    private static String buildSizeMessage(String assetName, int expectedWidth, int expectedHeight,
                                          int actualWidth, int actualHeight) {
        return String.format("Asset validation failed: %s - Expected %dx%d, got %dx%d",
                           assetName, expectedWidth, expectedHeight, actualWidth, actualHeight);
    }
    
    private static String buildHashMessage(String assetName, String expectedHash, String actualHash) {
        return String.format("Asset validation failed: %s - Hash mismatch\nExpected: %s\nActual: %s",
                           assetName, expectedHash, actualHash);
    }
    
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("Asset Validation Error\n");
        sb.append("======================\n");
        sb.append("Asset: ").append(assetName).append("\n");
        sb.append("Type: ").append(validationType).append("\n");
        
        if (validationType == ValidationType.SIZE_MISMATCH) {
            sb.append("Expected Size: ").append(expectedWidth).append("x").append(expectedHeight).append("\n");
            sb.append("Actual Size: ").append(actualWidth).append("x").append(actualHeight).append("\n");
        } else if (validationType == ValidationType.HASH_MISMATCH) {
            sb.append("Expected Hash: ").append(expectedHash).append("\n");
            sb.append("Actual Hash: ").append(actualHash).append("\n");
        }
        
        return sb.toString();
    }
    
    // Getters
    public String getAssetName() {
        return assetName;
    }
    
    public ValidationType getValidationType() {
        return validationType;
    }
    
    public int getExpectedWidth() {
        return expectedWidth;
    }
    
    public int getExpectedHeight() {
        return expectedHeight;
    }
    
    public int getActualWidth() {
        return actualWidth;
    }
    
    public int getActualHeight() {
        return actualHeight;
    }
    
    public String getExpectedHash() {
        return expectedHash;
    }
    
    public String getActualHash() {
        return actualHash;
    }
}
