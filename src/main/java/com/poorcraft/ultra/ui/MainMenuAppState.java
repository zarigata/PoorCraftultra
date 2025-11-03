package com.poorcraft.ultra.ui;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.texture.Texture;
import com.poorcraft.ultra.player.GameSessionAppState;
import com.poorcraft.ultra.tools.AssetValidator;
import com.simsilica.lemur.*;
import com.simsilica.lemur.style.BaseStyles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main menu AppState with animated background (CP v0.2).
 * Displays main menu with Lemur UI and animated 3D background.
 */
public class MainMenuAppState extends AbstractAppState {
    
    private static final Logger logger = LoggerFactory.getLogger(MainMenuAppState.class);
    
    private Application app;
    private Node rootNode;
    private Node guiNode;
    private AssetManager assetManager;
    
    private Container menuContainer;
    private Node backgroundNode;
    
    private static final float ROTATION_SPEED = 0.15f; // radians per second
    
    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        
        this.app = app;
        SimpleApplication simpleApp = (SimpleApplication) app;
        this.rootNode = simpleApp.getRootNode();
        this.guiNode = simpleApp.getGuiNode();
        this.assetManager = app.getAssetManager();
        
        // Initialize Lemur
        GuiGlobals.initialize(app);
        BaseStyles.loadGlassStyle();
        GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");
        
        // Create animated background
        createAnimatedBackground();
        
        // Create menu UI
        createMenuUI();
    }
    
    private void createAnimatedBackground() {
        backgroundNode = new Node("MenuBackground");
        
        // Create box geometry
        Box box = new Box(5f, 5f, 5f);
        Geometry boxGeom = new Geometry("BackgroundBox", box);
        
        // Load generated block texture
        try {
            Texture texture = assetManager.loadTexture("blocks/stone_01.png");
            Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setTexture("ColorMap", texture);
            boxGeom.setMaterial(mat);
        } catch (Exception e) {
            // Fallback to solid color if texture not found
            Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", ColorRGBA.Gray);
            boxGeom.setMaterial(mat);
        }
        
        backgroundNode.attachChild(boxGeom);
        
        // Position camera to view box
        SimpleApplication simpleApp = (SimpleApplication) app;
        simpleApp.getCamera().setLocation(new Vector3f(0, 0, 15));
        simpleApp.getCamera().lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
        
        // Add directional light
        DirectionalLight light = new DirectionalLight();
        light.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        light.setColor(ColorRGBA.White);
        backgroundNode.addLight(light);
        
        // Attach to root node
        rootNode.attachChild(backgroundNode);
    }
    
    private void createMenuUI() {
        int width = app.getContext().getSettings().getWidth();
        int height = app.getContext().getSettings().getHeight();
        
        // Create main container
        menuContainer = new Container();
        
        // Title
        Label titleLabel = new Label("Poorcraft Ultra");
        titleLabel.setFontSize(48);
        titleLabel.setColor(ColorRGBA.White);
        menuContainer.addChild(titleLabel);
        
        // Version
        Label versionLabel = new Label("v2.2.0-SNAPSHOT - CP v2.2");
        versionLabel.setFontSize(20);
        versionLabel.setColor(ColorRGBA.LightGray);
        menuContainer.addChild(versionLabel);
        
        // Assets OK badge
        if (AssetValidator.getValidationStatus() == AssetValidator.ValidationStatus.OK) {
            Label assetsOkLabel = new Label("\u2713 Assets OK");
            assetsOkLabel.setFontSize(18);
            assetsOkLabel.setColor(ColorRGBA.Green);
            menuContainer.addChild(assetsOkLabel);
        }
        
        // Spacer
        menuContainer.addChild(new Label(" "));
        
        // Buttons
        Button singleplayerButton = new Button("Singleplayer");
        singleplayerButton.setFontSize(24);
        singleplayerButton.addClickCommands(source -> {
            logger.info("Opening world creation dialog...");
            showWorldCreationDialog();
        });
        menuContainer.addChild(singleplayerButton);
        
        Button optionsButton = new Button("Options");
        optionsButton.setEnabled(false); // Disabled for CP v0.2
        optionsButton.setFontSize(24);
        menuContainer.addChild(optionsButton);
        
        Button quitButton = new Button("Quit");
        quitButton.setFontSize(24);
        quitButton.addClickCommands(source -> app.stop());
        menuContainer.addChild(quitButton);
        
        // Center container on screen
        menuContainer.setLocalTranslation(
            (width - menuContainer.getPreferredSize().x) / 2,
            (height + menuContainer.getPreferredSize().y) / 2,
            1 // Z-index above background
        );
        
        // Attach to GUI node
        guiNode.attachChild(menuContainer);
    }
    
    private void showWorldCreationDialog() {
        int width = app.getContext().getSettings().getWidth();
        int height = app.getContext().getSettings().getHeight();
        
        // Create modal container
        Container dialogContainer = new Container();
        
        // Title
        Label titleLabel = new Label("Create New World");
        titleLabel.setFontSize(32);
        titleLabel.setColor(ColorRGBA.White);
        dialogContainer.addChild(titleLabel);
        
        // Spacer
        dialogContainer.addChild(new Label(" "));
        
        // Seed label
        Label seedLabel = new Label("World Seed (leave empty for random):");
        seedLabel.setFontSize(18);
        seedLabel.setColor(ColorRGBA.White);
        dialogContainer.addChild(seedLabel);
        
        // Seed text field
        TextField seedField = new TextField("");
        seedField.setPreferredWidth(300);
        seedField.setFontSize(20);
        dialogContainer.addChild(seedField);
        
        // Spacer
        dialogContainer.addChild(new Label(" "));
        
        // Buttons container
        Container buttonsContainer = new Container();
        
        Button createButton = new Button("Create World");
        createButton.setFontSize(20);
        createButton.addClickCommands(source -> {
            // Parse seed
            long seed;
            String seedText = seedField.getText().trim();
            if (seedText.isEmpty()) {
                seed = System.currentTimeMillis();
                logger.info("Using random seed: {}", seed);
            } else {
                try {
                    seed = Long.parseLong(seedText);
                } catch (NumberFormatException e) {
                    // Use hash of string as seed
                    seed = seedText.hashCode();
                    logger.info("Using string hash as seed: {} -> {}", seedText, seed);
                }
            }
            
            // Close dialog
            guiNode.detachChild(dialogContainer);
            
            // Detach menu
            app.getStateManager().detach(MainMenuAppState.this);
            
            // Attach game session with seed
            GameSessionAppState gameSession = new GameSessionAppState(seed);
            app.getStateManager().attach(gameSession);
        });
        buttonsContainer.addChild(createButton);
        
        Button cancelButton = new Button("Cancel");
        cancelButton.setFontSize(20);
        cancelButton.addClickCommands(source -> {
            guiNode.detachChild(dialogContainer);
        });
        buttonsContainer.addChild(cancelButton);
        
        dialogContainer.addChild(buttonsContainer);
        
        // Center dialog on screen
        dialogContainer.setLocalTranslation(
            (width - dialogContainer.getPreferredSize().x) / 2,
            (height + dialogContainer.getPreferredSize().y) / 2,
            2 // Z-index above menu
        );
        
        // Attach to GUI node
        guiNode.attachChild(dialogContainer);
    }
    
    @Override
    public void update(float tpf) {
        super.update(tpf);
        
        // Rotate background
        if (backgroundNode != null) {
            backgroundNode.rotate(0, ROTATION_SPEED * tpf, 0);
        }
    }
    
    @Override
    public void cleanup() {
        super.cleanup();
        
        // Detach menu container
        if (menuContainer != null && guiNode != null) {
            guiNode.detachChild(menuContainer);
        }
        
        // Detach background
        if (backgroundNode != null && rootNode != null) {
            rootNode.detachChild(backgroundNode);
        }
        
        // Null references
        menuContainer = null;
        backgroundNode = null;
        rootNode = null;
        guiNode = null;
        assetManager = null;
        app = null;
    }
}
