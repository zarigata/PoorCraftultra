package com.poorcraft.ultra.engine;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.poorcraft.ultra.engine.api.InputMappings;
import com.poorcraft.ultra.shared.Config;
import com.poorcraft.ultra.shared.Logger;
import com.poorcraft.ultra.gameplay.Inventory;
import com.poorcraft.ultra.player.PlayerInteractionState;
import com.poorcraft.ultra.ui.HudState;
import com.poorcraft.ultra.ui.HotbarUI;
import com.poorcraft.ultra.ui.MainMenuState;
import com.poorcraft.ultra.voxel.BlockRegistry;
import com.poorcraft.ultra.voxel.BlockType;
import com.poorcraft.ultra.voxel.ChunkRenderer;
import com.poorcraft.ultra.voxel.ChunkStorage;
import com.poorcraft.ultra.voxel.SuperchunkTestState;
import com.poorcraft.ultra.voxel.TextureAtlas;
import com.poorcraft.ultra.world.WorldSaveManager;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;

public final class EngineCore extends SimpleApplication {

    private static final Logger logger = Logger.getLogger(EngineCore.class);
    private static final ColorRGBA SKYBOX_COLOR = new ColorRGBA(0.529f, 0.808f, 0.922f, 1.0f);

    private final Config config;
    private final CountDownLatch stopLatch = new CountDownLatch(1);

    private CameraController cameraController;
    private boolean wireframeEnabled;
    private Inventory playerInventory;
    private PlayerInteractionState playerInteraction;
    private WorldSaveManager worldSaveManager;

    private final ActionListener toggleMenuListener = (name, isPressed, tpf) -> {
        if (!InputMappings.TOGGLE_MENU.equals(name) || !isPressed) {
            return;
        }

        MainMenuState menuState = stateManager.getState(MainMenuState.class);
        if (menuState == null) {
            logger.warn("ToggleMenu invoked but MainMenuState is not attached");
            return;
        }

        boolean enableMenu = !menuState.isEnabled();
        menuState.setEnabled(enableMenu);

        if (enableMenu) {
            logger.info("Menu opened (ESC)");
        } else {
            logger.info("Menu closed (ESC)");
        }
    };

    public EngineCore(Config config) {
        this.config = Objects.requireNonNull(config, "config must not be null");
    }

    @Override
    public void simpleInitApp() {
        logger.info("Engine initialized");

        flyCam.setEnabled(false);
        logger.info("Default flyCam disabled");

        cam.setLocation(new Vector3f(64f, 10f, 64f));
        cam.lookAt(new Vector3f(64f, 0f, 64f), Vector3f.UNIT_Y);
        logger.info("Camera spawned at (64, 10, 64) looking toward superchunk center");

        viewPort.setBackgroundColor(SKYBOX_COLOR);
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.5f, -1.0f, -0.5f).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun);
        logger.info("3D scene initialized (skybox + directional light)");

        PhysicsManager physicsManager = new PhysicsManager();
        stateManager.attach(physicsManager);
        logger.info("Physics manager attached");

        cameraController = new CameraController();
        stateManager.attach(cameraController);
        logger.info("Camera controller attached");

        HudState hudState = new HudState();
        stateManager.attach(hudState);
        logger.info("HUD attached");

        SuperchunkTestState voxelState = new SuperchunkTestState();
        stateManager.attach(voxelState);
        logger.info("Superchunk test state attached");

        playerInventory = new Inventory();
        logger.info("Player inventory created");
        playerInventory.addItem(BlockType.STONE, 64);
        playerInventory.addItem(BlockType.DIRT, 64);
        playerInventory.addItem(BlockType.GRASS, 64);
        playerInventory.addItem(BlockType.PLANKS, 64);
        logger.info("Starter blocks added to inventory");

        HotbarUI hotbarUI = new HotbarUI(playerInventory);
        stateManager.attach(hotbarUI);
        logger.info("Hotbar UI attached");

        playerInteraction = new PlayerInteractionState(playerInventory);
        stateManager.attach(playerInteraction);
        logger.info("Player interaction attached");

        initializeWorldSaveManager(voxelState);

        if (TextureAtlas.isInitialized()) {
            TextureAtlas atlas = TextureAtlas.getInstance();
            BlockRegistry registry = BlockRegistry.load(atlas);
            logger.info("Block registry loaded ({} blocks)", registry.getBlockCount());
        } else {
            logger.warn("TextureAtlas not initialized; skipping block registry load");
        }

        wireframeEnabled = config.hasPath("debug.wireframe") && config.getBoolean("debug.wireframe");

        registerInputMappings();
        logger.info("Input mappings registered");

        enqueue(() -> {
            if (ChunkRenderer.isInitialized()) {
                ChunkRenderer.getInstance().setWireframe(wireframeEnabled);
            }
            return null;
        });

        logger.banner("CP v1.3 OK – Poorcraft Ultra");
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (worldSaveManager != null && WorldSaveManager.isInitialized()) {
            worldSaveManager.update(tpf);
        }
    }

    @Override
    public void destroy() {
        if (worldSaveManager != null && WorldSaveManager.isInitialized()) {
            try {
                logger.info("Saving world on exit...");
                worldSaveManager.shutdown();
                logger.info("World saved successfully");
            } catch (Exception e) {
                logger.error("Failed to save world on exit", e);
            }
        }
        super.destroy();
        stopLatch.countDown();
    }

    public void awaitStop() throws InterruptedException {
        stopLatch.await();
    }

    private void initializeWorldSaveManager(SuperchunkTestState voxelState) {
        if (voxelState == null) {
            logger.warn("Voxel state unavailable; skipping world save manager initialization");
            return;
        }

        ChunkStorage storage = voxelState.getChunkStorage();
        if (storage == null) {
            logger.warn("Chunk storage unavailable; skipping world save manager initialization");
            return;
        }

        worldSaveManager = WorldSaveManager.initialize(storage);
        logger.info("World save manager initialized");
    }

    private void registerInputMappings() {
        var inputManager = getInputManager();
        if (inputManager == null) {
            logger.warn("InputManager not available; skipping input registration");
            return;
        }

        inputManager.addMapping(InputMappings.MOVE_FORWARD, new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping(InputMappings.MOVE_BACKWARD, new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping(InputMappings.MOVE_LEFT, new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping(InputMappings.MOVE_RIGHT, new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping(InputMappings.MOVE_UP, new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping(InputMappings.MOVE_DOWN, new KeyTrigger(KeyInput.KEY_LSHIFT));

        inputManager.addMapping(InputMappings.MOUSE_X_POS, new MouseAxisTrigger(MouseInput.AXIS_X, false));
        inputManager.addMapping(InputMappings.MOUSE_X_NEG, new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping(InputMappings.MOUSE_Y_POS, new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        inputManager.addMapping(InputMappings.MOUSE_Y_NEG, new MouseAxisTrigger(MouseInput.AXIS_Y, true));

        inputManager.addMapping(InputMappings.TOGGLE_MENU, new KeyTrigger(KeyInput.KEY_ESCAPE));
        inputManager.addMapping(InputMappings.TOGGLE_WIREFRAME, new KeyTrigger(KeyInput.KEY_F3));
        inputManager.addMapping(InputMappings.BREAK_BLOCK, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping(InputMappings.PLACE_BLOCK, new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addMapping(InputMappings.SELECT_SLOT_1, new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping(InputMappings.SELECT_SLOT_2, new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping(InputMappings.SELECT_SLOT_3, new KeyTrigger(KeyInput.KEY_3));
        inputManager.addMapping(InputMappings.SELECT_SLOT_4, new KeyTrigger(KeyInput.KEY_4));
        inputManager.addMapping(InputMappings.SELECT_SLOT_5, new KeyTrigger(KeyInput.KEY_5));
        inputManager.addMapping(InputMappings.SELECT_SLOT_6, new KeyTrigger(KeyInput.KEY_6));
        inputManager.addMapping(InputMappings.SELECT_SLOT_7, new KeyTrigger(KeyInput.KEY_7));
        inputManager.addMapping(InputMappings.SELECT_SLOT_8, new KeyTrigger(KeyInput.KEY_8));
        inputManager.addMapping(InputMappings.SELECT_SLOT_9, new KeyTrigger(KeyInput.KEY_9));

        inputManager.addListener(cameraController,
                InputMappings.MOVE_FORWARD,
                InputMappings.MOVE_BACKWARD,
                InputMappings.MOVE_LEFT,
                InputMappings.MOVE_RIGHT,
                InputMappings.MOVE_UP,
                InputMappings.MOVE_DOWN);

        inputManager.addListener(cameraController,
                InputMappings.MOUSE_X_POS,
                InputMappings.MOUSE_X_NEG,
                InputMappings.MOUSE_Y_POS,
                InputMappings.MOUSE_Y_NEG);
        inputManager.addListener(toggleMenuListener, InputMappings.TOGGLE_MENU);
        inputManager.addListener(toggleWireframeListener, InputMappings.TOGGLE_WIREFRAME);
        if (playerInteraction != null) {
            inputManager.addListener(playerInteraction,
                    InputMappings.BREAK_BLOCK,
                    InputMappings.PLACE_BLOCK,
                    InputMappings.SELECT_SLOT_1,
                    InputMappings.SELECT_SLOT_2,
                    InputMappings.SELECT_SLOT_3,
                    InputMappings.SELECT_SLOT_4,
                    InputMappings.SELECT_SLOT_5,
                    InputMappings.SELECT_SLOT_6,
                    InputMappings.SELECT_SLOT_7,
                    InputMappings.SELECT_SLOT_8,
                    InputMappings.SELECT_SLOT_9);
        }
    }

    private final ActionListener toggleWireframeListener = (name, isPressed, tpf) -> {
        if (!InputMappings.TOGGLE_WIREFRAME.equals(name) || !isPressed) {
            return;
        }

        wireframeEnabled = !wireframeEnabled;
        if (ChunkRenderer.isInitialized()) {
            ChunkRenderer.getInstance().setWireframe(wireframeEnabled);
        }
        logger.info("Wireframe toggled {}", wireframeEnabled ? "on" : "off");
    };
}

