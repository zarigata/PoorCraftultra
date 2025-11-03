package com.poorcraft.ultra.smelting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Manager for block entities (furnaces, chests, etc.).
 * CP v3.3: Smelting System
 */
public class BlockEntityManager {
    private static final Logger LOGGER = Logger.getLogger(BlockEntityManager.class.getName());
    
    private final Map<BlockPos, FurnaceBlockEntity> blockEntities = new HashMap<>();
    private final List<FurnaceBlockEntity> activeEntities = new ArrayList<>();
    private final int tickBudget = 10;  // Max entities to tick per frame
    private float tickAccumulator = 0.0f;
    
    /**
     * Immutable position record.
     */
    public record BlockPos(int x, int y, int z) {}
    
    /**
     * Adds a block entity at the given position.
     */
    public void addBlockEntity(int x, int y, int z, FurnaceBlockEntity entity) {
        BlockPos pos = new BlockPos(x, y, z);
        blockEntities.put(pos, entity);
        activeEntities.add(entity);
        LOGGER.fine("Added furnace at (" + x + ", " + y + ", " + z + ")");
    }
    
    /**
     * Removes a block entity at the given position.
     */
    public void removeBlockEntity(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        FurnaceBlockEntity entity = blockEntities.remove(pos);
        if (entity != null) {
            activeEntities.remove(entity);
            LOGGER.fine("Removed furnace at (" + x + ", " + y + ", " + z + ")");
        }
    }
    
    /**
     * Gets a block entity at the given position.
     */
    public FurnaceBlockEntity getBlockEntity(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        return blockEntities.get(pos);
    }
    
    /**
     * Checks if a block entity exists at the given position.
     */
    public boolean hasBlockEntity(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        return blockEntities.containsKey(pos);
    }
    
    /**
     * Updates all active block entities (called every frame).
     */
    public void update(float tpf) {
        tickAccumulator += tpf;
        
        // Tick at 20 ticks per second (0.05 seconds per tick)
        if (tickAccumulator >= 0.05f) {
            tickAccumulator -= 0.05f;
            
            // Process up to tickBudget entities
            int processed = 0;
            for (FurnaceBlockEntity entity : activeEntities) {
                entity.tick();
                processed++;
                if (processed >= tickBudget && activeEntities.size() > tickBudget) {
                    LOGGER.warning("Block entity tick backlog: " + (activeEntities.size() - processed) + " entities");
                    break;
                }
            }
        }
    }
    
    /**
     * Returns all block entities (for serialization).
     */
    public Map<BlockPos, FurnaceBlockEntity> getAllBlockEntities() {
        return new HashMap<>(blockEntities);
    }
}
