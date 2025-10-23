package com.poorcraftultra.world.block;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BlockProperties value class.
 */
class BlockPropertiesTest {

    @Test
    @DisplayName("Solid properties should have correct values")
    void testSolidProperties() {
        BlockProperties props = BlockProperties.solid();
        
        assertTrue(props.isSolid(), "Solid properties should be solid");
        assertFalse(props.isTransparent(), "Solid properties should not be transparent");
        assertFalse(props.isLightEmitting(), "Solid properties should not emit light");
        assertFalse(props.hasGravity(), "Solid properties should not have gravity");
        assertEquals(0, props.getLightLevel(), "Solid properties should have light level 0");
    }

    @Test
    @DisplayName("Transparent properties should have correct values")
    void testTransparentProperties() {
        BlockProperties props = BlockProperties.transparent();
        
        assertTrue(props.isSolid(), "Transparent properties should be solid");
        assertTrue(props.isTransparent(), "Transparent properties should be transparent");
        assertFalse(props.isLightEmitting(), "Transparent properties should not emit light");
        assertFalse(props.hasGravity(), "Transparent properties should not have gravity");
    }

    @Test
    @DisplayName("Air properties should have correct values")
    void testAirProperties() {
        BlockProperties props = BlockProperties.air();
        
        assertFalse(props.isSolid(), "Air properties should not be solid");
        assertTrue(props.isTransparent(), "Air properties should be transparent");
        assertFalse(props.isLightEmitting(), "Air properties should not emit light");
        assertFalse(props.hasGravity(), "Air properties should not have gravity");
    }

    @Test
    @DisplayName("Light emitting properties should work correctly")
    void testLightEmitting() {
        BlockProperties props = new BlockProperties(true, false, true, 15, false);
        
        assertTrue(props.isLightEmitting(), "Should be light emitting");
        assertEquals(15, props.getLightLevel(), "Should have light level 15");
    }

    @Test
    @DisplayName("Gravity properties should work correctly")
    void testGravity() {
        BlockProperties props = new BlockProperties(true, false, false, 0, true);
        
        assertTrue(props.hasGravity(), "Should have gravity");
    }

    @Test
    @DisplayName("Equals and hashCode should work correctly")
    void testEqualsAndHashCode() {
        BlockProperties props1 = new BlockProperties(true, false, false, 0, false);
        BlockProperties props2 = new BlockProperties(true, false, false, 0, false);
        BlockProperties props3 = new BlockProperties(false, true, false, 0, false);
        
        assertEquals(props1, props2, "Identical properties should be equal");
        assertEquals(props1.hashCode(), props2.hashCode(), "Identical properties should have same hash code");
        assertNotEquals(props1, props3, "Different properties should not be equal");
    }

    @Test
    @DisplayName("Factory methods should return correct property sets")
    void testFactoryMethods() {
        BlockProperties solid = BlockProperties.solid();
        BlockProperties transparent = BlockProperties.transparent();
        BlockProperties air = BlockProperties.air();
        
        assertTrue(solid.isSolid() && !solid.isTransparent());
        assertTrue(transparent.isSolid() && transparent.isTransparent());
        assertTrue(!air.isSolid() && air.isTransparent());
    }
}
