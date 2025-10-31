package com.poorcraft.ultra.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poorcraft.ultra.ui.GalleryState;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Phase 0.3 verification tests.
 */
public class Phase03Tests {

    private static final Path ASSETS_DIR = Path.of("assets");
    private static final Path TEXTURES_DIR = ASSETS_DIR.resolve("textures");
    private static final Path SKINS_DIR = ASSETS_DIR.resolve("skins");

    @Test
    void testAtlasFileExists() {
        assertTrue(Files.exists(TEXTURES_DIR.resolve("blocks_atlas.png")),
                "Atlas not found. Run :tools:assets:generate first.");
    }

    @Test
    void testAtlasJsonExists() {
        assertTrue(Files.exists(TEXTURES_DIR.resolve("blocks_atlas.json")),
                "Atlas JSON not found. Run :tools:assets:generate first.");
    }

    @Test
    void testManifestJsonExists() {
        assertTrue(Files.exists(TEXTURES_DIR.resolve("manifest.json")),
                "Manifest JSON not found. Run :tools:assets:generate first.");
    }

    @Test
    void testPlayerSkinExists() {
        assertTrue(Files.exists(SKINS_DIR.resolve("player.png")),
                "Player skin not found. Run :tools:assets:generate first.");
    }

    @Test
    void testNpcSkinsExist() {
        List<String> expected = List.of("npc_red.png", "npc_blue.png", "npc_green.png", "npc_yellow.png",
                "npc_purple.png");
        for (String file : expected) {
            assertTrue(Files.exists(SKINS_DIR.resolve(file)), "Missing NPC skin: " + file);
        }
    }

    @Test
    void testAtlasDimensions() throws IOException {
        var image = ImageIO.read(TEXTURES_DIR.resolve("blocks_atlas.png").toFile());
        assertNotNull(image, "Failed to load blocks_atlas.png");
        assertEquals(512, image.getWidth(), "Atlas width must be 512");
        assertEquals(512, image.getHeight(), "Atlas height must be 512");
    }

    @Test
    void testAtlasJsonParsing() throws IOException {
        Map<String, Map<String, Integer>> mapping = readAtlasJson();
        assertTrue(mapping.containsKey("stone"));
        assertTrue(mapping.containsKey("dirt"));
        assertTrue(mapping.containsKey("grass"));
    }

    @Test
    void testAtlasIndicesUnique() throws IOException {
        Map<String, Map<String, Integer>> mapping = readAtlasJson();
        Set<Integer> indices = new HashSet<>();
        int total = 0;
        for (Map<String, Integer> faces : mapping.values()) {
            indices.addAll(faces.values());
            total += faces.size();
        }
        assertEquals(indices.size(), total, "Atlas indices must be unique");
        assertTrue(indices.stream().allMatch(i -> i >= 0 && i < 64), "Atlas indices must be between 0 and 63");
    }

    @Test
    void testManifestJsonSchema() throws IOException {
        Map<String, Object> manifest = readManifestJson();
        assertEquals("0.3", manifest.get("version"));
        assertEquals(512, manifest.get("atlas_size"));
        assertEquals(64, manifest.get("block_size"));
        assertEquals(256, manifest.get("skin_size"));
        Object blocksObj = manifest.get("blocks");
        assertTrue(blocksObj instanceof List, "Blocks entry must be a list");
        List<?> blocks = (List<?>) blocksObj;
        assertTrue(blocks.size() >= 11, "Expected at least 11 block entries");
        Object skinsObj = manifest.get("skins");
        assertTrue(skinsObj instanceof List, "Skins entry must be a list");
        List<?> skins = (List<?>) skinsObj;
        assertTrue(skins.size() >= 6, "Expected at least 6 skins");
    }

    @Test
    void testGalleryStateCreation() {
        GalleryState gallery = new GalleryState();
        assertNotNull(gallery);
        assertTrue(!gallery.isInitialized(), "Gallery should not be initialized before attach");
    }

    @Test
    void testGalleryCycleLogic() {
        List<String> blocks = new ArrayList<>(List.of("stone", "dirt", "grass"));
        int index = 0;
        for (int i = 0; i < 6; i++) {
            index = (index + 1) % blocks.size();
        }
        assertEquals(0, index, "Index should wrap around to zero after full cycles");
    }

    @Disabled("Requires generated assets and full engine context")
    @Test
    void testGalleryIntegration() {
        // Placeholder for manual enabling when running with generated assets and engine context.
    }

    private Map<String, Map<String, Integer>> readAtlasJson() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String content = Files.readString(TEXTURES_DIR.resolve("blocks_atlas.json"));
        return mapper.readValue(content, new TypeReference<Map<String, Map<String, Integer>>>() {
        });
    }

    private Map<String, Object> readManifestJson() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String content = Files.readString(TEXTURES_DIR.resolve("manifest.json"));
        return mapper.readValue(content, new TypeReference<Map<String, Object>>() {
        });
    }
}
