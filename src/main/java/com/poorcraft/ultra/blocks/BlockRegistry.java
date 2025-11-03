package com.poorcraft.ultra.blocks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Central registry for all block types (singleton pattern).
 */
public class BlockRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(BlockRegistry.class);
    
    private static BlockRegistry instance;
    
    private final Map<Short, BlockDefinition> blocks = new HashMap<>();
    private final Map<String, BlockDefinition> blocksByName = new HashMap<>();
    
    public static final BlockDefinition AIR = BlockDefinition.builder()
            .id(0)
            .name("air")
            .solid(false)
            .transparent(true)
            .texture("air.png")
            .build();
    
    private BlockRegistry() {
        // Private constructor for singleton
    }
    
    public static BlockRegistry getInstance() {
        if (instance == null) {
            instance = new BlockRegistry();
            instance.init();
        }
        return instance;
    }
    
    /**
     * Initializes and registers all blocks.
     */
    private void init() {
        logger.info("Initializing BlockRegistry...");
        
        // Air (ID 0)
        register(AIR);
        
        // Stone variants (1-3)
        register(BlockDefinition.builder()
                .id(1).name("stone")
                .texture("stone_01.png")
                .hardness(1.5f)
                .build());
        
        register(BlockDefinition.builder()
                .id(2).name("cobblestone")
                .texture("cobblestone.png")
                .hardness(2.0f)
                .build());
        
        register(BlockDefinition.builder()
                .id(3).name("stone_bricks")
                .texture("stone_bricks.png")
                .hardness(1.5f)
                .build());
        
        // Natural blocks (4-7)
        register(BlockDefinition.builder()
                .id(4).name("dirt")
                .texture("dirt.png")
                .hardness(0.5f)
                .build());
        
        register(BlockDefinition.builder()
                .id(5).name("grass")
                .topBottomSide("grass_top.png", "dirt.png", "grass_side.png")
                .hardness(0.6f)
                .drops(4) // Drops dirt
                .build());
        
        register(BlockDefinition.builder()
                .id(6).name("sand")
                .texture("sand.png")
                .hardness(0.5f)
                .build());
        
        register(BlockDefinition.builder()
                .id(7).name("gravel")
                .texture("gravel.png")
                .hardness(0.6f)
                .build());
        
        // Wood blocks (8-13)
        register(BlockDefinition.builder()
                .id(8).name("oak_planks")
                .texture("oak_planks.png")
                .hardness(2.0f)
                .build());
        
        register(BlockDefinition.builder()
                .id(9).name("birch_planks")
                .texture("birch_planks.png")
                .hardness(2.0f)
                .build());
        
        register(BlockDefinition.builder()
                .id(10).name("spruce_planks")
                .texture("spruce_planks.png")
                .hardness(2.0f)
                .build());
        
        register(BlockDefinition.builder()
                .id(11).name("oak_log")
                .topBottomSide("wood_log_top.png", "wood_log_top.png", "wood_log_side.png")
                .hardness(2.0f)
                .build());
        
        // Ores (12-17)
        register(BlockDefinition.builder()
                .id(12).name("coal_ore")
                .texture("coal_ore.png")
                .hardness(3.0f)
                .build());
        
        register(BlockDefinition.builder()
                .id(13).name("iron_ore")
                .texture("iron_ore.png")
                .hardness(3.0f)
                .build());
        
        register(BlockDefinition.builder()
                .id(14).name("gold_ore")
                .texture("gold_ore.png")
                .hardness(3.0f)
                .build());
        
        register(BlockDefinition.builder()
                .id(15).name("diamond_ore")
                .texture("diamond_ore.png")
                .hardness(3.0f)
                .build());
        
        register(BlockDefinition.builder()
                .id(16).name("redstone_ore")
                .texture("redstone_ore.png")
                .hardness(3.0f)
                .build());
        
        register(BlockDefinition.builder()
                .id(17).name("emerald_ore")
                .texture("emerald_ore.png")
                .hardness(3.0f)
                .build());
        
        // Decorative (18-20)
        register(BlockDefinition.builder()
                .id(18).name("glass")
                .texture("glass.png")
                .hardness(0.3f)
                .transparent(true)
                .build());
        
        register(BlockDefinition.builder()
                .id(19).name("oak_leaves")
                .texture("oak_leaves.png")
                .hardness(0.2f)
                .transparent(true)
                .build());
        
        register(BlockDefinition.builder()
                .id(20).name("birch_leaves")
                .texture("birch_leaves.png")
                .hardness(0.2f)
                .transparent(true)
                .build());
        
        // Special (21-23)
        register(BlockDefinition.builder()
                .id(21).name("crafting_table")
                .topBottomSide("crafting_table_top.png", "oak_planks.png", "crafting_table_side.png")
                .hardness(2.5f)
                .build());
        
        register(BlockDefinition.builder()
                .id(22).name("furnace")
                .topBottomSide("furnace_top.png", "furnace_top.png", "furnace_front.png")
                .hardness(3.5f)
                .build());
        
        register(BlockDefinition.builder()
                .id(23).name("chest")
                .texture("chest_front.png")
                .hardness(2.5f)
                .build());
        
        // Light-emitting blocks (24-26)
        register(BlockDefinition.builder()
                .id(24).name("torch")
                .texture("torch.png")
                .hardness(0.0f)
                .transparent(true)
                .solid(false)
                .lightEmission(14)
                .build());
        
        register(BlockDefinition.builder()
                .id(25).name("lava")
                .texture("lava.png")
                .hardness(100.0f)
                .transparent(false)
                .lightEmission(15)
                .build());
        
        register(BlockDefinition.builder()
                .id(26).name("glowstone")
                .texture("glowstone.png")
                .hardness(0.3f)
                .lightEmission(15)
                .build());
        
        logger.info("BlockRegistry initialized: {} blocks", blocks.size());
    }
    
    /**
     * Registers a block definition.
     */
    public void register(BlockDefinition block) {
        blocks.put(block.getId(), block);
        blocksByName.put(block.getName(), block);
        logger.debug("Registered block: {} (ID {})", block.getName(), block.getId());
    }
    
    /**
     * Returns block definition by ID (returns AIR if not found).
     */
    public BlockDefinition getBlock(short id) {
        return blocks.getOrDefault(id, AIR);
    }
    
    /**
     * Returns block definition by name.
     */
    public BlockDefinition getBlock(String name) {
        return blocksByName.get(name);
    }
    
    /**
     * Returns collection of all registered blocks.
     */
    public Collection<BlockDefinition> getAllBlocks() {
        return blocks.values();
    }
    
    /**
     * Returns number of registered blocks.
     */
    public int getBlockCount() {
        return blocks.size();
    }
}
