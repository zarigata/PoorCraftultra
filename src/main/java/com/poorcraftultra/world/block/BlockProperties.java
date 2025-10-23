package com.poorcraftultra.world.block;

import java.util.Objects;

/**
 * Immutable value class representing the properties of a block type.
 * <p>
 * Properties define the behavior and characteristics of blocks:
 * <ul>
 *   <li><b>solid</b>: Blocks movement and light propagation (used now for collision and rendering)</li>
 *   <li><b>transparent</b>: Allows light through and affects face culling (used now for rendering)</li>
 *   <li><b>lightEmitting</b>: Emits light (reserved for future lighting system)</li>
 *   <li><b>lightLevel</b>: Light emission level 0-15 (reserved for future lighting system)</li>
 *   <li><b>hasGravity</b>: Falls like sand/gravel (reserved for future physics phase)</li>
 * </ul>
 * <p>
 * This class uses value semantics with proper equals/hashCode implementation.
 */
public class BlockProperties {
    private final boolean solid;
    private final boolean transparent;
    private final boolean lightEmitting;
    private final int lightLevel;
    private final boolean hasGravity;

    /**
     * Creates a new BlockProperties instance with all properties specified.
     *
     * @param solid         whether the block blocks movement and light
     * @param transparent   whether the block allows light through
     * @param lightEmitting whether the block emits light
     * @param lightLevel    light emission level (0-15)
     * @param hasGravity    whether the block is affected by gravity
     */
    public BlockProperties(boolean solid, boolean transparent, boolean lightEmitting, int lightLevel, boolean hasGravity) {
        this.solid = solid;
        this.transparent = transparent;
        this.lightEmitting = lightEmitting;
        this.lightLevel = Math.max(0, Math.min(15, lightLevel));
        this.hasGravity = hasGravity;
    }

    /**
     * @return true if the block blocks movement and light
     */
    public boolean isSolid() {
        return solid;
    }

    /**
     * @return true if the block allows light through (affects face culling)
     */
    public boolean isTransparent() {
        return transparent;
    }

    /**
     * @return true if the block emits light (reserved for future)
     */
    public boolean isLightEmitting() {
        return lightEmitting;
    }

    /**
     * @return light emission level 0-15 (reserved for future)
     */
    public int getLightLevel() {
        return lightLevel;
    }

    /**
     * @return true if the block is affected by gravity (reserved for future)
     */
    public boolean hasGravity() {
        return hasGravity;
    }

    /**
     * Factory method for solid, opaque blocks (like stone, dirt).
     *
     * @return properties for a solid, opaque block
     */
    public static BlockProperties solid() {
        return new BlockProperties(true, false, false, 0, false);
    }

    /**
     * Factory method for solid but transparent blocks (like glass).
     *
     * @return properties for a transparent block
     */
    public static BlockProperties transparent() {
        return new BlockProperties(true, true, false, 0, false);
    }

    /**
     * Factory method for air blocks (non-solid, transparent).
     *
     * @return properties for air
     */
    public static BlockProperties air() {
        return new BlockProperties(false, true, false, 0, false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockProperties that = (BlockProperties) o;
        return solid == that.solid &&
                transparent == that.transparent &&
                lightEmitting == that.lightEmitting &&
                lightLevel == that.lightLevel &&
                hasGravity == that.hasGravity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(solid, transparent, lightEmitting, lightLevel, hasGravity);
    }

    @Override
    public String toString() {
        return "BlockProperties{" +
                "solid=" + solid +
                ", transparent=" + transparent +
                ", lightEmitting=" + lightEmitting +
                ", lightLevel=" + lightLevel +
                ", hasGravity=" + hasGravity +
                '}';
    }
}
