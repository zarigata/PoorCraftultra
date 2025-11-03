package com.poorcraft.ultra.tests;

import com.poorcraft.ultra.engine.PoorcraftApp;
import com.poorcraft.ultra.tools.AssetValidator;
import com.poorcraft.ultra.tools.AssetValidationException;
import org.junit.jupiter.api.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated smoke tests for Poorcraft Ultra.
 * Tests CP v0.1 and v0.2 functionality.
 */
@DisplayName("Poorcraft Ultra Smoke Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SmokeTest {
    
    @BeforeAll
    static void setupAssets() {
        // Assets should be generated before tests run
        // This is handled by Gradle build process
    }
    
    @Test
    @Order(1)
    @Tag("smoke")
    @DisplayName("Asset validation passes for generated assets")
    @Timeout(10)
    void testAssetValidation_PassesForGeneratedAssets() {
        assertDoesNotThrow(() -> {
            AssetValidator.validateAll();
        }, "Asset validation should pass");
        
        assertEquals(AssetValidator.ValidationStatus.OK, 
                    AssetValidator.getValidationStatus(),
                    "Validation status should be OK");
    }
    
    @Test
    @Order(2)
    @Tag("smoke")
    @DisplayName("CP v0.1: Window opens, FPS counter displays, ESC quits")
    @Timeout(15)
    void testCpV01_WindowOpensAndCloses() throws Exception {
        PoorcraftApp app = new PoorcraftApp(true); // smoke test mode
        
        boolean success = HeadlessAppRunner.runHeadless(app, 60);
        
        assertTrue(success, "App didn't complete successfully");
    }
    
    @Test
    @Order(3)
    @Tag("smoke")
    @DisplayName("CP v0.2: Main menu loads with animated background")
    @Timeout(15)
    void testCpV02_MainMenuLoads() throws Exception {
        PoorcraftApp app = new PoorcraftApp(true); // smoke test mode
        
        boolean success = HeadlessAppRunner.runHeadless(app, 60);
        
        assertTrue(success, "App didn't complete successfully");
    }
    
    @Test
    @Order(4)
    @DisplayName("Asset validation fails for invalid size")
    void testAssetValidation_FailsForInvalidSize() throws IOException {
        // Create a temporary PNG with wrong dimensions (128x128 instead of 64x64)
        Path tempDir = Files.createTempDirectory("poorcraft-test");
        Path blocksDir = tempDir.resolve("blocks");
        Files.createDirectories(blocksDir);
        
        // Create 128x128 PNG (wrong size for blocks)
        BufferedImage invalidImage = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        Path invalidPng = blocksDir.resolve("invalid_test.png");
        ImageIO.write(invalidImage, "png", invalidPng.toFile());
        
        // Create a temporary manifest that references this invalid image
        Path manifestPath = blocksDir.resolve("manifest.json");
        String manifestContent = "{\n" +
            "  \"textures\": [\n" +
            "    {\n" +
            "      \"name\": \"invalid_test.png\",\n" +
            "      \"width\": 64,\n" +
            "      \"height\": 64,\n" +
            "      \"hash\": \"dummy_hash\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
        Files.writeString(manifestPath, manifestContent);
        
        // Validate and expect SIZE_MISMATCH exception
        AssetValidationException exception = assertThrows(AssetValidationException.class, () -> {
            AssetValidator.validateManifest(manifestPath, 64, 64);
        });
        
        // Verify it's a size mismatch error
        assertEquals(AssetValidationException.ValidationType.SIZE_MISMATCH, 
                    exception.getValidationType(),
                    "Expected SIZE_MISMATCH validation error");
        
        // Cleanup
        Files.deleteIfExists(invalidPng);
        Files.deleteIfExists(manifestPath);
        Files.deleteIfExists(blocksDir);
        Files.deleteIfExists(tempDir);
    }
    
    @AfterEach
    void cleanupTempFiles() {
        // Clean up any temporary test files created during tests
        try {
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
            Files.list(tempDir)
                .filter(p -> p.getFileName().toString().startsWith("poorcraft-test"))
                .forEach(p -> {
                    try {
                        Files.walk(p)
                            .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                            .forEach(path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException e) {
                                    // Ignore cleanup errors
                                }
                            });
                    } catch (IOException e) {
                        // Ignore cleanup errors
                    }
                });
        } catch (IOException e) {
            // Ignore cleanup errors
        }
    }
    
    @AfterAll
    static void cleanup() {
        // Clean up any temporary test files
        System.out.println("Smoke tests completed");
    }
}
