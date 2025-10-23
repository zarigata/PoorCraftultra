package com.poorcraftultra.world.block;

/**
 * Functional interface for listening to block interaction events.
 * <p>
 * This interface provides hooks for block placement, breaking, and updates.
 * It is designed as a foundation for future phases including:
 * <ul>
 *   <li>Block placement system (Phase 8)</li>
 *   <li>Mod API and custom block behaviors</li>
 *   <li>Block update propagation (e.g., for redstone-like mechanics)</li>
 * </ul>
 * <p>
 * Default implementations are provided so listeners can implement only the
 * methods they need.
 */
public interface BlockInteractionListener {

    /**
     * Called when a block is placed in the world.
     *
     * @param x       world X coordinate
     * @param y       world Y coordinate
     * @param z       world Z coordinate
     * @param blockId the ID of the placed block
     * @param block   the Block definition
     */
    default void onBlockPlaced(int x, int y, int z, byte blockId, Block block) {
        // Default: do nothing
    }

    /**
     * Called when a block is broken/removed from the world.
     *
     * @param x         world X coordinate
     * @param y         world Y coordinate
     * @param z         world Z coordinate
     * @param oldBlockId the ID of the broken block
     * @param oldBlock   the Block definition that was broken
     */
    default void onBlockBroken(int x, int y, int z, byte oldBlockId, Block oldBlock) {
        // Default: do nothing
    }

    /**
     * Called when a block changes (for future use in block update systems).
     *
     * @param x          world X coordinate
     * @param y          world Y coordinate
     * @param z          world Z coordinate
     * @param oldBlockId the previous block ID
     * @param newBlockId the new block ID
     */
    default void onBlockUpdated(int x, int y, int z, byte oldBlockId, byte newBlockId) {
        // Default: do nothing
    }
}
