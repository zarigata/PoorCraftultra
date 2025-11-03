package com.poorcraft.ultra.player;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.poorcraft.ultra.blocks.BlockRegistry;
import com.poorcraft.ultra.crafting.RecipeRegistry;
import com.poorcraft.ultra.inventory.ItemStack;
import com.poorcraft.ultra.inventory.PlayerInventory;
import com.poorcraft.ultra.items.ItemRegistry;
import com.poorcraft.ultra.save.PlayerData;
import com.poorcraft.ultra.save.SaveManager;
import com.poorcraft.ultra.smelting.BlockEntityManager;
import com.poorcraft.ultra.smelting.FuelRegistry;
import com.poorcraft.ultra.smelting.SmeltingRecipeRegistry;
import com.poorcraft.ultra.voxel.ChunkManager;
import com.poorcraft.ultra.weather.WeatherManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main in-game AppState (CP v1.05+).
 */
public class GameSessionAppState extends AbstractAppState implements ActionListener {
    
    private static final Logger logger = LoggerFactory.getLogger(GameSessionAppState.class);
    
    private SimpleApplication app;
    private AppStateManager stateManager;
    private BulletAppState bulletAppState;
    private ChunkManager chunkManager;
    private FirstPersonController firstPersonController;
    private BlockInteraction blockInteraction;
    private WeatherManager weatherManager;
    private DirectionalLight sun;
    
    // CP v3.1: Inventory system
    private PlayerInventory playerInventory;
    private BlockEntityManager blockEntityManager;
    private SaveManager saveManager;
    private com.poorcraft.ultra.inventory.InventoryAppState inventoryAppState;
    private com.poorcraft.ultra.smelting.FurnaceAppState furnaceAppState;
    
    private long worldSeed;
    private Vector3f spawnPosition = new Vector3f(8, 80, 8);
    private int frameCounter = 0;
    
    /**
     * Creates a game session with default seed.
     */
    public GameSessionAppState() {
        this(System.currentTimeMillis());
    }
    
    /**
     * Creates a game session with the specified world seed.
     */
    public GameSessionAppState(long worldSeed) {
        this.worldSeed = worldSeed;
    }
    
    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (SimpleApplication) app;
        this.stateManager = stateManager;
        
        logger.info("Initializing GameSessionAppState (CP v3.3) with seed {}...", worldSeed);
        
        // Initialize registries (CP v3.1-3.3)
        BlockRegistry.getInstance();
        ItemRegistry.getInstance();
        RecipeRegistry.getInstance();
        FuelRegistry.getInstance();
        SmeltingRecipeRegistry.getInstance();
        
        // Attach BulletAppState for physics
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        
        // Create and init ChunkManager with seed
        chunkManager = new ChunkManager();
        chunkManager.init(this.app.getAssetManager(), this.app.getRootNode(), worldSeed);
        
        // Create SaveManager
        saveManager = new SaveManager("world1", chunkManager);
        
        // Create BlockEntityManager (CP v3.3)
        blockEntityManager = new BlockEntityManager();
        
        // Load or create player data (CP v3.1)
        PlayerData playerData = saveManager.loadPlayerData();
        playerInventory = playerData.getInventory();
        
        // Give player some starting items for testing
        if (playerInventory.findEmptySlot() == 0) {
            playerInventory.addItem(ItemStack.of(1060, 64)); // stone
            playerInventory.addItem(ItemStack.of(1068, 64)); // oak planks
            playerInventory.addItem(ItemStack.of(1000, 1, 59)); // wooden pickaxe
            playerInventory.addItem(ItemStack.of(1026, 16)); // coal
            playerInventory.addItem(ItemStack.of(1071, 10)); // iron ore
        }
        
        // Create and init FirstPersonController
        firstPersonController = new FirstPersonController(this.app);
        firstPersonController.init(bulletAppState, spawnPosition);
        
        // Create and init BlockInteraction (CP v3.1: with inventory)
        blockInteraction = new BlockInteraction(chunkManager, this.app.getCamera(), playerInventory, blockEntityManager);
        blockInteraction.init(this.app.getRootNode(), this.app.getAssetManager());
        
        // Create directional light for sun
        sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.5f, -1.0f, -0.5f).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        this.app.getRootNode().addLight(sun);
        
        // Create and attach WeatherManager
        weatherManager = new WeatherManager(sun);
        stateManager.attach(weatherManager);
        
        // Set initial sky color (will be updated by WeatherManager)
        this.app.getViewPort().setBackgroundColor(new ColorRGBA(0.5f, 0.7f, 1.0f, 1.0f));
        
        // Set up input
        setupInput();
        
        // Register as listener for block interaction
        app.getInputManager().addListener(this, "BreakBlock", "PlaceBlock");
        
        // Add E key for inventory (CP v3.1)
        app.getInputManager().addMapping("ToggleInventory", new KeyTrigger(KeyInput.KEY_E));
        app.getInputManager().addListener(this, "ToggleInventory");
        
        logger.info("GameSessionAppState initialized");
    }
    
    private void setupInput() {
        // Register hotbar number keys (1-9)
        app.getInputManager().addMapping("Hotbar1", new KeyTrigger(KeyInput.KEY_1));
        app.getInputManager().addMapping("Hotbar2", new KeyTrigger(KeyInput.KEY_2));
        app.getInputManager().addMapping("Hotbar3", new KeyTrigger(KeyInput.KEY_3));
        app.getInputManager().addMapping("Hotbar4", new KeyTrigger(KeyInput.KEY_4));
        app.getInputManager().addMapping("Hotbar5", new KeyTrigger(KeyInput.KEY_5));
        app.getInputManager().addMapping("Hotbar6", new KeyTrigger(KeyInput.KEY_6));
        app.getInputManager().addMapping("Hotbar7", new KeyTrigger(KeyInput.KEY_7));
        app.getInputManager().addMapping("Hotbar8", new KeyTrigger(KeyInput.KEY_8));
        app.getInputManager().addMapping("Hotbar9", new KeyTrigger(KeyInput.KEY_9));
        
        app.getInputManager().addListener(this, "Hotbar1", "Hotbar2", "Hotbar3", 
            "Hotbar4", "Hotbar5", "Hotbar6", "Hotbar7", "Hotbar8", "Hotbar9");
    }
    
    
    @Override
    public void update(float tpf) {
        if (!isEnabled()) {
            return;
        }
        
        // Update chunk manager (processes dirty chunks)
        chunkManager.update(tpf);
        
        // Update first person controller
        if (firstPersonController != null) {
            firstPersonController.update(tpf);
        }
        
        // Update block interaction
        if (blockInteraction != null) {
            blockInteraction.update(tpf);
        }
        
        // Update block entity manager (CP v3.3)
        if (blockEntityManager != null) {
            blockEntityManager.update(tpf);
        }
        
        // Update chunks around player every 60 frames (CP v1.1+)
        frameCounter++;
        if (frameCounter >= 60 && firstPersonController != null) {
            chunkManager.updateChunksAroundPlayer(firstPersonController.getPosition());
            frameCounter = 0;
        }
    }
    
    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!isPressed) {
            return;
        }
        
        switch (name) {
            case "BreakBlock":
                if (blockInteraction != null) {
                    blockInteraction.breakBlock();
                }
                break;
            case "PlaceBlock":
                if (blockInteraction != null) {
                    handleRightClick();
                }
                break;
            case "Hotbar1":
                if (playerInventory != null) playerInventory.setSelectedSlot(0);
                break;
            case "Hotbar2":
                if (playerInventory != null) playerInventory.setSelectedSlot(1);
                break;
            case "Hotbar3":
                if (playerInventory != null) playerInventory.setSelectedSlot(2);
                break;
            case "Hotbar4":
                if (playerInventory != null) playerInventory.setSelectedSlot(3);
                break;
            case "Hotbar5":
                if (playerInventory != null) playerInventory.setSelectedSlot(4);
                break;
            case "Hotbar6":
                if (playerInventory != null) playerInventory.setSelectedSlot(5);
                break;
            case "Hotbar7":
                if (playerInventory != null) playerInventory.setSelectedSlot(6);
                break;
            case "Hotbar8":
                if (playerInventory != null) playerInventory.setSelectedSlot(7);
                break;
            case "Hotbar9":
                if (playerInventory != null) playerInventory.setSelectedSlot(8);
                break;
            case "ToggleInventory":
                toggleInventory();
                break;
        }
    }
    
    @Override
    public void cleanup() {
        super.cleanup();
        
        // Remove input listener
        app.getInputManager().removeListener(this);
        
        // Save player data (CP v3.1)
        if (saveManager != null && playerInventory != null) {
            PlayerData playerData = new PlayerData(playerInventory);
            saveManager.savePlayerData(playerData);
        }
        
        // Remove hotbar mappings
        app.getInputManager().deleteMapping("Hotbar1");
        app.getInputManager().deleteMapping("Hotbar2");
        app.getInputManager().deleteMapping("Hotbar3");
        app.getInputManager().deleteMapping("Hotbar4");
        app.getInputManager().deleteMapping("Hotbar5");
        app.getInputManager().deleteMapping("Hotbar6");
        app.getInputManager().deleteMapping("Hotbar7");
        app.getInputManager().deleteMapping("Hotbar8");
        app.getInputManager().deleteMapping("Hotbar9");
        app.getInputManager().deleteMapping("ToggleInventory");
        
        if (firstPersonController != null) {
            firstPersonController.cleanup();
        }
        
        if (chunkManager != null) {
            chunkManager.clear();
        }
        
        if (bulletAppState != null) {
            getStateManager().detach(bulletAppState);
        }
        
        logger.info("GameSessionAppState cleaned up");
    }
    
    // Accessors for testing
    public ChunkManager getChunkManager() {
        return chunkManager;
    }
    
    public BlockInteraction getBlockInteraction() {
        return blockInteraction;
    }
    
    public FirstPersonController getFirstPersonController() {
        return firstPersonController;
    }
    
    // CP v3.1: Inventory accessor
    public PlayerInventory getPlayerInventory() {
        return playerInventory;
    }
    
    // CP v3.3: BlockEntityManager accessor
    public BlockEntityManager getBlockEntityManager() {
        return blockEntityManager;
    }
    
    /**
     * Toggles inventory UI (CP v3.1).
     */
    private void toggleInventory() {
        if (inventoryAppState != null && inventoryAppState.isEnabled()) {
            // Close inventory
            stateManager.detach(inventoryAppState);
            inventoryAppState = null;
        } else if (furnaceAppState == null || !furnaceAppState.isEnabled()) {
            // Open inventory (only if furnace is not open)
            inventoryAppState = new com.poorcraft.ultra.inventory.InventoryAppState(playerInventory, firstPersonController);
            stateManager.attach(inventoryAppState);
            logger.info("Opened inventory");
        }
    }
    
    /**
     * Handles right-click: checks for interactive blocks, otherwise places block (CP v3.3).
     */
    private void handleRightClick() {
        if (blockInteraction == null) {
            return;
        }
        
        // Get targeted block
        BlockInteraction.BlockPos targetBlock = blockInteraction.getTargetBlock();
        if (targetBlock == null) {
            return;
        }
        
        short blockId = chunkManager.getBlock(targetBlock.x, targetBlock.y, targetBlock.z);
        
        // Check if block is interactive
        if (blockId == 22) { // Furnace
            if (blockEntityManager.hasBlockEntity(targetBlock.x, targetBlock.y, targetBlock.z)) {
                com.poorcraft.ultra.smelting.FurnaceBlockEntity furnace = 
                    blockEntityManager.getBlockEntity(targetBlock.x, targetBlock.y, targetBlock.z);
                openFurnace(furnace);
                return;
            }
        }
        // Future: crafting table (21), chest (23)
        
        // Not interactive, place block
        blockInteraction.placeBlock();
    }
    
    /**
     * Opens furnace UI (CP v3.3).
     */
    private void openFurnace(com.poorcraft.ultra.smelting.FurnaceBlockEntity furnace) {
        if (furnaceAppState != null && furnaceAppState.isEnabled()) {
            // Already open
            return;
        }
        
        // Close inventory if open
        if (inventoryAppState != null && inventoryAppState.isEnabled()) {
            stateManager.detach(inventoryAppState);
            inventoryAppState = null;
        }
        
        furnaceAppState = new com.poorcraft.ultra.smelting.FurnaceAppState(furnace, playerInventory, firstPersonController);
        stateManager.attach(furnaceAppState);
        logger.info("Opened furnace at ({}, {}, {})", furnace.getWorldX(), furnace.getWorldY(), furnace.getWorldZ());
    }
}
