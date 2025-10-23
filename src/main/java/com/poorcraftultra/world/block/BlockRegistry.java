package com.poorcraftultra.world.block;

import java.util.*;

/**
 * Singleton registry managing all block type definitions.
 * <p>
 * The registry maintains a mapping of block IDs (0-255) to Block instances and
 * provides lookup by ID or name. It is initialized with default blocks and can
 * be extended with custom blocks before being locked.
 * <p>
 * <b>Thread Safety:</b> This class is NOT thread-safe. It should only be accessed
 * from the main thread during initialization.
 * <p>
 * <b>Usage:</b>
 * <pre>
 * BlockRegistry registry = BlockRegistry.getInstance();
 * Block stone = registry.getBlock((byte) 1);
 * Block grass = registry.getBlock("grass");
 * </pre>
 */
public class BlockRegistry {
    private static final BlockRegistry INSTANCE = new BlockRegistry();

    private final Block[] blocks;
    private final Map<String, Block> blocksByName;
    private boolean locked;

    /**
     * Private constructor initializes the registry and registers default blocks.
     */
    private BlockRegistry() {
        this.blocks = new Block[256];
        this.blocksByName = new HashMap<>();
        this.locked = false;

        registerDefaultBlocks();
    }

    /**
     * Returns the singleton instance of the BlockRegistry.
     *
     * @return the BlockRegistry instance
     */
    public static BlockRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a block in the registry.
     *
     * @param block the block to register
     * @throws IllegalStateException    if the registry is locked
     * @throws IllegalArgumentException if a block with the same ID or name already exists
     */
    public void register(Block block) {
        if (locked) {
            throw new IllegalStateException("Cannot register blocks after registry is locked");
        }

        int id = block.getId() & 0xFF; // Convert byte to unsigned int

        if (blocks[id] != null) {
            throw new IllegalArgumentException("Block ID " + id + " is already registered: " + blocks[id].getName());
        }

        if (blocksByName.containsKey(block.getName())) {
            throw new IllegalArgumentException("Block name '" + block.getName() + "' is already registered");
        }

        blocks[id] = block;
        blocksByName.put(block.getName(), block);
    }

    /**
     * Gets a block by its ID.
     *
     * @param id the block ID
     * @return the block, or AIR (ID 0) if not found
     */
    public Block getBlock(byte id) {
        int index = id & 0xFF; // Convert byte to unsigned int
        Block block = blocks[index];
        return block != null ? block : blocks[0]; // Return AIR if not found
    }

    /**
     * Gets a block by its name.
     *
     * @param name the block name
     * @return the block, or null if not found
     */
    public Block getBlock(String name) {
        return blocksByName.get(name);
    }

    /**
     * Checks if a block ID is registered.
     *
     * @param id the block ID
     * @return true if the block is registered
     */
    public boolean isRegistered(byte id) {
        int index = id & 0xFF;
        return blocks[index] != null;
    }

    /**
     * Returns all registered blocks.
     *
     * @return an unmodifiable collection of all blocks
     */
    public Collection<Block> getAllBlocks() {
        return Collections.unmodifiableCollection(blocksByName.values());
    }

    /**
     * Locks the registry to prevent further registration.
     * Should be called after all blocks are registered during initialization.
     */
    public void lock() {
        this.locked = true;
    }

    /**
     * @return true if the registry is locked
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * Registers the default blocks that are always available.
     */
    private void registerDefaultBlocks() {
        // AIR (ID 0) - non-solid, transparent, no texture
        register(new Block(
                (byte) 0,
                "air",
                "Air",
                BlockProperties.air(),
                ""
        ));

        // STONE (ID 1) - solid, opaque, "stone" texture on all faces
        register(new Block(
                (byte) 1,
                "stone",
                "Stone",
                BlockProperties.solid(),
                "stone"
        ));

        // GRASS (ID 2) - solid, opaque, different textures per face
        register(new Block(
                (byte) 2,
                "grass",
                "Grass Block",
                BlockProperties.solid(),
                new String[]{"grass_top", "dirt", "grass_side", "grass_side", "grass_side", "grass_side"}
        ));

        // DIRT (ID 3) - solid, opaque, "dirt" texture on all faces
        register(new Block(
                (byte) 3,
                "dirt",
                "Dirt",
                BlockProperties.solid(),
                "dirt"
        ));

        // SAND (ID 4) - solid, opaque, "sand" texture, has gravity (for future)
        register(new Block(
                (byte) 4,
                "sand",
                "Sand",
                new BlockProperties(true, false, false, 0, true),
                "sand"
        ));

        // GLASS (ID 5) - solid, transparent, "glass" texture
        register(new Block(
                (byte) 5,
                "glass",
                "Glass",
                BlockProperties.transparent(),
                "glass"
        ));
    }
}
