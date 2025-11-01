package com.poorcraft.ultra.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AssetManifest JSON parsing.
 */
class AssetManifestTest {
    
    @Test
    void testParseValidManifest() {
        String json = """
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
                },
                {
                  "name": "player_base",
                  "category": "skins",
                  "path": "skins/player_base.png",
                  "width": 256,
                  "height": 256,
                  "sha256": "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210"
                }
              ]
            }
            """;
        
        AssetManifest manifest = AssetManifest.fromJson(json);
        
        assertNotNull(manifest);
        assertEquals("1.0", manifest.version());
        assertEquals("2025-01-15T10:30:00Z", manifest.generated());
        assertEquals(42, manifest.seed());
        assertEquals(2, manifest.assets().size());
        
        // Check first entry
        AssetManifest.AssetEntry entry1 = manifest.assets().get(0);
        assertEquals("wood_oak", entry1.name());
        assertEquals("blocks", entry1.category());
        assertEquals("blocks/wood_oak.png", entry1.path());
        assertEquals(64, entry1.width());
        assertEquals(64, entry1.height());
        assertEquals(64, entry1.sha256().length());
        
        // Check second entry
        AssetManifest.AssetEntry entry2 = manifest.assets().get(1);
        assertEquals("player_base", entry2.name());
        assertEquals("skins", entry2.category());
        assertEquals(256, entry2.width());
        assertEquals(256, entry2.height());
    }
    
    @Test
    void testParseMalformedJson() {
        String malformedJson = "{ invalid json }";
        
        assertThrows(IllegalArgumentException.class, () -> {
            AssetManifest.fromJson(malformedJson);
        });
    }
    
    @Test
    void testParseMissingRequiredFields() {
        // Missing "version" field
        String jsonMissingVersion = """
            {
              "generated": "2025-01-15T10:30:00Z",
              "seed": 42,
              "assets": []
            }
            """;
        
        assertThrows(IllegalArgumentException.class, () -> {
            AssetManifest.fromJson(jsonMissingVersion);
        });
        
        // Missing "assets" field
        String jsonMissingAssets = """
            {
              "version": "1.0",
              "generated": "2025-01-15T10:30:00Z",
              "seed": 42
            }
            """;
        
        assertThrows(IllegalArgumentException.class, () -> {
            AssetManifest.fromJson(jsonMissingAssets);
        });
    }
    
    @Test
    void testParseEmptyAssets() {
        String json = """
            {
              "version": "1.0",
              "generated": "2025-01-15T10:30:00Z",
              "seed": 42,
              "assets": []
            }
            """;
        
        AssetManifest manifest = AssetManifest.fromJson(json);
        
        assertNotNull(manifest);
        assertEquals("1.0", manifest.version());
        assertEquals(0, manifest.assets().size());
    }
    
    @Test
    void testAssetEntryValidation() {
        // Invalid SHA-256 hash (too short)
        String jsonInvalidHash = """
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
                  "sha256": "tooshort"
                }
              ]
            }
            """;
        
        assertThrows(IllegalArgumentException.class, () -> {
            AssetManifest.fromJson(jsonInvalidHash);
        });
    }
}
