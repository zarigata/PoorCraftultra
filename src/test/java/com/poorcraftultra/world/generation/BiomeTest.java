package com.poorcraftultra.world.generation;

import com.poorcraftultra.world.block.BlockRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Biome enum.
 */
class BiomeTest {
    
    @BeforeAll
    static void setup() {
        // Ensure BlockRegistry is initialized
        BlockRegistry.getInstance();
    }
    
    @Test
    @DisplayName("All biomes have valid properties")
    void testBiomeProperties() {
        for (Biome biome : Biome.values()) {
            // Temperature and humidity should be in [0, 1]
            assertTrue(biome.getBaseTemperature() >= 0.0 && biome.getBaseTemperature() <= 1.0,
                    biome.getName() + " has invalid temperature: " + biome.getBaseTemperature());
            assertTrue(biome.getBaseHumidity() >= 0.0 && biome.getBaseHumidity() <= 1.0,
                    biome.getName() + " has invalid humidity: " + biome.getBaseHumidity());
            
            // Block IDs should not be null (byte 0 is AIR, which is valid)
            assertNotNull(biome.getSurfaceBlock(), biome.getName() + " has null surface block");
            assertNotNull(biome.getSubsurfaceBlock(), biome.getName() + " has null subsurface block");
            assertNotNull(biome.getStoneBlock(), biome.getName() + " has null stone block");
            
            // Name should not be empty
            assertNotNull(biome.getName(), biome.getName() + " has null name");
            assertFalse(biome.getName().isEmpty(), biome.getName() + " has empty name");
        }
    }
    
    @Test
    @DisplayName("Biome selection works correctly for known temperature/humidity values")
    void testBiomeSelection() {
        // Hot and dry -> DESERT
        Biome desert = Biome.fromTemperatureHumidity(0.9, 0.1);
        assertEquals(Biome.DESERT, desert, "Hot and dry should select DESERT");
        
        // Cold -> SNOW
        Biome snow = Biome.fromTemperatureHumidity(0.1, 0.5);
        assertEquals(Biome.SNOW, snow, "Cold should select SNOW");
        
        // Hot and humid -> JUNGLE
        Biome jungle = Biome.fromTemperatureHumidity(0.9, 0.9);
        assertEquals(Biome.JUNGLE, jungle, "Hot and humid should select JUNGLE");
        
        // Temperate -> PLAINS
        Biome plains = Biome.fromTemperatureHumidity(0.5, 0.5);
        assertEquals(Biome.PLAINS, plains, "Temperate should select PLAINS");
        
        // Cool -> MOUNTAINS
        Biome mountains = Biome.fromTemperatureHumidity(0.3, 0.4);
        assertEquals(Biome.MOUNTAINS, mountains, "Cool should select MOUNTAINS");
    }
    
    @Test
    @DisplayName("All biome blocks are registered in BlockRegistry")
    void testBiomeBlockIds() {
        BlockRegistry registry = BlockRegistry.getInstance();
        
        for (Biome biome : Biome.values()) {
            assertTrue(registry.isRegistered(biome.getSurfaceBlock()),
                    biome.getName() + " surface block not registered: " + biome.getSurfaceBlock());
            assertTrue(registry.isRegistered(biome.getSubsurfaceBlock()),
                    biome.getName() + " subsurface block not registered: " + biome.getSubsurfaceBlock());
            assertTrue(registry.isRegistered(biome.getStoneBlock()),
                    biome.getName() + " stone block not registered: " + biome.getStoneBlock());
        }
    }
    
    @Test
    @DisplayName("Biome height modifiers are reasonable")
    void testBiomeHeightModifiers() {
        // MOUNTAINS should have the highest variation
        assertTrue(Biome.MOUNTAINS.getHeightVariation() > Biome.PLAINS.getHeightVariation(),
                "MOUNTAINS should have higher variation than PLAINS");
        assertTrue(Biome.MOUNTAINS.getHeightVariation() > Biome.DESERT.getHeightVariation(),
                "MOUNTAINS should have higher variation than DESERT");
        
        // MOUNTAINS should have positive height offset
        assertTrue(Biome.MOUNTAINS.getHeightOffset() > 0,
                "MOUNTAINS should have positive height offset");
        
        // DESERT should have negative or zero height offset
        assertTrue(Biome.DESERT.getHeightOffset() <= 0,
                "DESERT should have negative or zero height offset");
    }
    
    @Test
    @DisplayName("All biomes have unique temperature/humidity combinations")
    void testAllBiomesUnique() {
        Biome[] biomes = Biome.values();
        for (int i = 0; i < biomes.length; i++) {
            for (int j = i + 1; j < biomes.length; j++) {
                Biome b1 = biomes[i];
                Biome b2 = biomes[j];
                
                // At least one of temperature or humidity should be different
                boolean different = b1.getBaseTemperature() != b2.getBaseTemperature() ||
                                  b1.getBaseHumidity() != b2.getBaseHumidity();
                assertTrue(different, b1.getName() + " and " + b2.getName() + 
                          " have identical temperature/humidity");
            }
        }
    }
    
    @Test
    @DisplayName("All biomes have non-empty display names")
    void testBiomeNames() {
        for (Biome biome : Biome.values()) {
            assertNotNull(biome.getName(), biome + " has null name");
            assertFalse(biome.getName().isEmpty(), biome + " has empty name");
            assertTrue(biome.getName().length() > 0, biome + " has zero-length name");
        }
    }
    
    @Test
    @DisplayName("Biome selection is deterministic")
    void testBiomeSelectionDeterminism() {
        // Same input should always produce same output
        for (int i = 0; i < 100; i++) {
            double temp = i / 100.0;
            double humid = (100 - i) / 100.0;
            
            Biome first = Biome.fromTemperatureHumidity(temp, humid);
            Biome second = Biome.fromTemperatureHumidity(temp, humid);
            
            assertEquals(first, second, 
                    "Biome selection not deterministic for temp=" + temp + ", humid=" + humid);
        }
    }
}
