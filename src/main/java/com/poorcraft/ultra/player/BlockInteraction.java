package com.poorcraft.ultra.player;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.poorcraft.ultra.blocks.BlockDefinition;
import com.poorcraft.ultra.blocks.BlockFace;
import com.poorcraft.ultra.blocks.BlockRegistry;
import com.poorcraft.ultra.inventory.ItemStack;
import com.poorcraft.ultra.inventory.PlayerInventory;
import com.poorcraft.ultra.items.ItemDefinition;
import com.poorcraft.ultra.items.ItemRegistry;
import com.poorcraft.ultra.smelting.BlockEntityManager;
import com.poorcraft.ultra.smelting.FurnaceBlockEntity;
import com.poorcraft.ultra.voxel.ChunkManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Block placement and breaking via ray-picking.
 */
public class BlockInteraction {
    
    private static final Logger logger = LoggerFactory.getLogger(BlockInteraction.class);
    
    private final ChunkManager chunkManager;
    private final BlockRegistry blockRegistry;
    private final Camera camera;
    private final PlayerInventory playerInventory;
    private final ItemRegistry itemRegistry;
    private final BlockEntityManager blockEntityManager;
    
    private float maxReach = 6.0f;
    
    private Geometry highlightGeometry;
    private BlockPos targetBlock;
    private BlockFace targetFace;
    
    public BlockInteraction(ChunkManager chunkManager, Camera camera, PlayerInventory playerInventory, BlockEntityManager blockEntityManager) {
        this.chunkManager = chunkManager;
        this.blockRegistry = BlockRegistry.getInstance();
        this.camera = camera;
        this.playerInventory = playerInventory;
        this.itemRegistry = ItemRegistry.getInstance();
        this.blockEntityManager = blockEntityManager;
    }
    
    /**
     * Initializes interaction system.
     */
    public void init(Node rootNode, AssetManager assetManager) {
        logger.info("Initializing BlockInteraction...");
        
        // Create highlight box
        Box box = new Box(0.505f, 0.505f, 0.505f);
        highlightGeometry = new Geometry("BlockHighlight", box);
        
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(1, 1, 1, 0.5f));
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        mat.getAdditionalRenderState().setWireframe(true);
        mat.getAdditionalRenderState().setLineWidth(2.0f);
        
        highlightGeometry.setMaterial(mat);
        highlightGeometry.setCullHint(Spatial.CullHint.Always);
        
        rootNode.attachChild(highlightGeometry);
        
        logger.info("BlockInteraction initialized");
    }
    
    /**
     * Update loop - performs ray-picking.
     */
    public void update(float tpf) {
        updateTargetBlock();
    }
    
    private void updateTargetBlock() {
        Vector3f origin = camera.getLocation();
        Vector3f direction = camera.getDirection();
        
        // 3D DDA ray marching
        RaycastResult result = raycast(origin, direction, maxReach);
        
        if (result != null) {
            targetBlock = result.blockPos;
            targetFace = result.face;
            
            // Show highlight box
            highlightGeometry.setLocalTranslation(
                targetBlock.x + 0.5f,
                targetBlock.y + 0.5f,
                targetBlock.z + 0.5f
            );
            highlightGeometry.setCullHint(Spatial.CullHint.Dynamic);
        } else {
            targetBlock = null;
            targetFace = null;
            highlightGeometry.setCullHint(Spatial.CullHint.Always);
        }
    }
    
    /**
     * 3D DDA ray marching for voxel picking.
     */
    private RaycastResult raycast(Vector3f origin, Vector3f direction, float maxDistance) {
        // Current voxel position
        int x = (int) Math.floor(origin.x);
        int y = (int) Math.floor(origin.y);
        int z = (int) Math.floor(origin.z);
        
        // Direction signs
        int stepX = direction.x > 0 ? 1 : -1;
        int stepY = direction.y > 0 ? 1 : -1;
        int stepZ = direction.z > 0 ? 1 : -1;
        
        // Distance to next voxel boundary
        float tMaxX = intbound(origin.x, direction.x);
        float tMaxY = intbound(origin.y, direction.y);
        float tMaxZ = intbound(origin.z, direction.z);
        
        // Distance between voxel boundaries
        float tDeltaX = stepX / direction.x;
        float tDeltaY = stepY / direction.y;
        float tDeltaZ = stepZ / direction.z;
        
        BlockFace lastFace = BlockFace.NORTH;
        
        // March along ray
        float t = 0;
        while (t < maxDistance) {
            // Check current voxel
            short blockId = chunkManager.getBlock(x, y, z);
            if (blockId != 0) {
                BlockDefinition block = blockRegistry.getBlock(blockId);
                if (block.isSolid()) {
                    return new RaycastResult(new BlockPos(x, y, z), lastFace);
                }
            }
            
            // Step to next voxel
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    x += stepX;
                    t = tMaxX;
                    tMaxX += tDeltaX;
                    lastFace = stepX > 0 ? BlockFace.WEST : BlockFace.EAST;
                } else {
                    z += stepZ;
                    t = tMaxZ;
                    tMaxZ += tDeltaZ;
                    lastFace = stepZ > 0 ? BlockFace.NORTH : BlockFace.SOUTH;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    y += stepY;
                    t = tMaxY;
                    tMaxY += tDeltaY;
                    lastFace = stepY > 0 ? BlockFace.DOWN : BlockFace.UP;
                } else {
                    z += stepZ;
                    t = tMaxZ;
                    tMaxZ += tDeltaZ;
                    lastFace = stepZ > 0 ? BlockFace.NORTH : BlockFace.SOUTH;
                }
            }
        }
        
        return null;
    }
    
    private float intbound(float s, float ds) {
        if (ds < 0) {
            return intbound(-s, -ds);
        } else {
            s = mod(s, 1);
            return (1 - s) / ds;
        }
    }
    
    private float mod(float value, float modulus) {
        return (value % modulus + modulus) % modulus;
    }
    
    /**
     * Breaks targeted block (CP v3.1: with tool effectiveness and drops).
     */
    public void breakBlock() {
        if (targetBlock == null) {
            return;
        }
        
        short blockId = chunkManager.getBlock(targetBlock.x, targetBlock.y, targetBlock.z);
        if (blockId == 0) {
            return;
        }
        
        BlockDefinition block = blockRegistry.getBlock(blockId);
        
        // Get selected tool
        ItemStack selectedStack = playerInventory.getSelectedStack();
        ItemDefinition selectedItem = selectedStack.isEmpty() ? null : selectedStack.getItem();
        
        // Calculate break time (for future animation)
        float baseTime = block.getHardness() * 1.5f;
        if (selectedItem != null && selectedItem.isTool()) {
            if (selectedItem.getToolType().isEffectiveAgainst(blockId)) {
                baseTime /= (selectedItem.getToolTier().getMiningSpeed() * 1.5f);
            } else {
                baseTime *= 5.0f; // Penalty for wrong tool
            }
        }
        
        // Check if block is furnace and remove block entity
        if (blockId == 22) { // Furnace block ID
            if (blockEntityManager.hasBlockEntity(targetBlock.x, targetBlock.y, targetBlock.z)) {
                FurnaceBlockEntity furnace = blockEntityManager.getBlockEntity(targetBlock.x, targetBlock.y, targetBlock.z);
                // Drop furnace contents
                if (!furnace.getInputSlot().isEmpty()) {
                    playerInventory.addItem(furnace.getInputSlot());
                }
                if (!furnace.getFuelSlot().isEmpty()) {
                    playerInventory.addItem(furnace.getFuelSlot());
                }
                if (!furnace.getOutputSlot().isEmpty()) {
                    playerInventory.addItem(furnace.getOutputSlot());
                }
                blockEntityManager.removeBlockEntity(targetBlock.x, targetBlock.y, targetBlock.z);
                logger.info("Broke furnace at ({}, {}, {})", targetBlock.x, targetBlock.y, targetBlock.z);
            }
        }
        
        // Break block
        chunkManager.setBlock(targetBlock.x, targetBlock.y, targetBlock.z, (short) 0);
        
        // Get drops (map block ID to item ID)
        short droppedBlockId = block.getDrops();
        if (droppedBlockId > 0) {
            int droppedItemId = mapBlockIdToItemId(droppedBlockId);
            ItemStack droppedStack = ItemStack.of(droppedItemId, 1);
            ItemStack remaining = playerInventory.addItem(droppedStack);
            if (!remaining.isEmpty()) {
                logger.warn("Inventory full, lost {} items", remaining.getCount());
            }
        }
        
        // Damage tool
        if (selectedItem != null && selectedItem.isTool()) {
            ItemStack damagedTool = selectedStack.damageTool(1);
            if (damagedTool.getDurability() <= 0) {
                // Tool broke
                playerInventory.setSelectedStack(ItemStack.empty());
                logger.info("Tool broke: {}", selectedItem.getName());
            } else {
                playerInventory.setSelectedStack(damagedTool);
            }
        }
        
        logger.info("Broke block at ({}, {}, {}): {}", 
            targetBlock.x, targetBlock.y, targetBlock.z, block.getName());
    }
    
    /**
     * Places selected block (CP v3.1: from inventory).
     */
    public void placeBlock() {
        if (targetBlock == null || targetFace == null) {
            return;
        }
        
        // Get selected item
        ItemStack selectedStack = playerInventory.getSelectedStack();
        if (selectedStack.isEmpty()) {
            return;
        }
        
        ItemDefinition selectedItem = selectedStack.getItem();
        if (!selectedItem.isPlaceable()) {
            return; // Not a placeable item
        }
        
        // Calculate placement position
        Vector3f normal = targetFace.getNormal();
        int placeX = targetBlock.x + (int) normal.x;
        int placeY = targetBlock.y + (int) normal.y;
        int placeZ = targetBlock.z + (int) normal.z;
        
        // Check if position is valid (restrict to chunk height)
        if (placeY < 0 || placeY >= com.poorcraft.ultra.voxel.ChunkConstants.CHUNK_SIZE_Y) {
            logger.debug("Cannot place block at Y={}: outside world height (0-{})", 
                placeY, com.poorcraft.ultra.voxel.ChunkConstants.CHUNK_SIZE_Y - 1);
            return;
        }
        
        short existingBlock = chunkManager.getBlock(placeX, placeY, placeZ);
        if (existingBlock != 0) {
            return; // Can't place in solid block
        }
        
        // Place block
        short blockId = selectedItem.getPlaceableBlock();
        chunkManager.setBlock(placeX, placeY, placeZ, blockId);
        
        // Create block entity if furnace
        if (blockId == 22) { // Furnace block ID
            FurnaceBlockEntity furnace = new FurnaceBlockEntity(placeX, placeY, placeZ);
            blockEntityManager.addBlockEntity(placeX, placeY, placeZ, furnace);
            logger.info("Placed furnace at ({}, {}, {})", placeX, placeY, placeZ);
        }
        
        // Decrement stack
        playerInventory.setSelectedStack(selectedStack.shrink(1));
        
        BlockDefinition block = blockRegistry.getBlock(blockId);
        logger.info("Placed block at ({}, {}, {}): {} (from item {})", 
            placeX, placeY, placeZ, block.getName(), selectedItem.getName());
    }
    
    /**
     * Interacts with targeted block (CP v3.3: opens furnace UI).
     */
    public void interactWithBlock() {
        if (targetBlock == null) {
            return;
        }
        
        short blockId = chunkManager.getBlock(targetBlock.x, targetBlock.y, targetBlock.z);
        if (blockId == 0) {
            return;
        }
        
        // Check if block is furnace
        if (blockId == 22) { // Furnace block ID
            if (blockEntityManager.hasBlockEntity(targetBlock.x, targetBlock.y, targetBlock.z)) {
                FurnaceBlockEntity furnace = blockEntityManager.getBlockEntity(targetBlock.x, targetBlock.y, targetBlock.z);
                logger.info("Opening furnace at ({}, {}, {})", targetBlock.x, targetBlock.y, targetBlock.z);
                // Note: FurnaceAppState attachment will be handled by GameSessionAppState
            }
        }
        
        // Future: crafting table (block ID 21), chest (block ID 23)
    }
    
    public BlockEntityManager getBlockEntityManager() {
        return blockEntityManager;
    }
    
    public BlockPos getTargetBlock() {
        return targetBlock;
    }
    
    /**
     * Maps block ID to corresponding item ID.
     * Most blocks have a 1:1 mapping via block items (IDs 1060+).
     */
    private int mapBlockIdToItemId(short blockId) {
        // Block items start at ID 1060 and follow block order
        // Block ID 1 (stone) -> Item ID 1060
        // Block ID 2 (cobblestone) -> Item ID 1061
        // etc.
        
        // Special cases for blocks that don't drop themselves
        switch (blockId) {
            case 1: return 1060;  // stone
            case 2: return 1061;  // cobblestone
            case 3: return 1062;  // stone_bricks
            case 4: return 1063;  // dirt
            case 5: return 1064;  // grass (but usually drops dirt, ID 4)
            case 6: return 1065;  // sand
            case 7: return 1066;  // gravel
            case 8: return 1067;  // oak_log
            case 9: return 1068;  // oak_planks
            case 10: return 1069; // oak_leaves
            case 12: return 1070; // coal_ore (drops coal item 1026, not ore block)
            case 13: return 1071; // iron_ore
            case 14: return 1072; // gold_ore
            case 15: return 1073; // diamond_ore (drops diamond item 1029, not ore block)
            case 16: return 1074; // redstone_ore (drops redstone item 1030, not ore block)
            case 17: return 1075; // emerald_ore (drops emerald item 1031, not ore block)
            case 18: return 1080; // glass
            case 21: return 1081; // crafting_table
            case 22: return 1082; // furnace
            case 23: return 1083; // chest
            case 24: return 1084; // torch
            default:
                logger.warn("Unknown block ID for item mapping: {}", blockId);
                return blockId; // Fallback to block ID
        }
    }
    
    public static class BlockPos {
        public final int x, y, z;
        
        public BlockPos(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
    
    private static class RaycastResult {
        final BlockPos blockPos;
        final BlockFace face;
        
        RaycastResult(BlockPos blockPos, BlockFace face) {
            this.blockPos = blockPos;
            this.face = face;
        }
    }
}
