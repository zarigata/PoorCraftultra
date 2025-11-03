package com.poorcraft.ultra.smelting;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.scene.Node;
import com.jme3.ui.Picture;
import com.poorcraft.ultra.inventory.ItemStack;
import com.poorcraft.ultra.inventory.PlayerInventory;
import com.poorcraft.ultra.items.ItemDefinition;
import com.poorcraft.ultra.player.FirstPersonController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Furnace UI (CP v3.3).
 * Displays input, fuel, output slots and progress bars.
 */
public class FurnaceAppState extends AbstractAppState implements ActionListener {
    
    private static final Logger logger = LoggerFactory.getLogger(FurnaceAppState.class);
    
    private SimpleApplication app;
    private AppStateManager stateManager;
    private Node guiNode;
    private FurnaceBlockEntity furnace;
    private PlayerInventory playerInventory;
    private FirstPersonController controller;
    
    private Node furnacePanel;
    private ItemStack heldStack = ItemStack.empty();
    private int hoveredSlot = -1; // -1=none, 0=input, 1=fuel, 2=output
    private BitmapFont font;
    
    // Layout constants
    private static final int SLOT_SIZE = 40;
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 250;
    
    public FurnaceAppState(FurnaceBlockEntity furnace, PlayerInventory playerInventory, FirstPersonController controller) {
        this.furnace = furnace;
        this.playerInventory = playerInventory;
        this.controller = controller;
    }
    
    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (SimpleApplication) app;
        this.stateManager = stateManager;
        this.guiNode = this.app.getGuiNode();
        
        logger.info("Initializing FurnaceAppState...");
        
        // Load font
        font = this.app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        
        // Disable FirstPersonController input
        if (controller != null) {
            controller.setInputEnabled(false);
        }
        
        // Show cursor
        app.getInputManager().setCursorVisible(true);
        
        // Create UI
        createFurnaceUI();
        
        // Set up input
        app.getInputManager().addMapping("CloseFurnace", new KeyTrigger(KeyInput.KEY_ESCAPE));
        app.getInputManager().addMapping("FurnaceClick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        app.getInputManager().addMapping("FurnaceRightClick", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        app.getInputManager().addListener(this, "CloseFurnace", "FurnaceClick", "FurnaceRightClick");
        
        logger.info("FurnaceAppState initialized");
    }
    
    private void createFurnaceUI() {
        furnacePanel = new Node("FurnacePanel");
        
        int screenWidth = app.getCamera().getWidth();
        int screenHeight = app.getCamera().getHeight();
        int panelX = (screenWidth - PANEL_WIDTH) / 2;
        int panelY = (screenHeight - PANEL_HEIGHT) / 2;
        
        // Background panel
        Picture background = new Picture("FurnaceBackground");
        background.setImage(app.getAssetManager(), "Textures/UI/furnace_bg.png", true);
        background.setWidth(PANEL_WIDTH);
        background.setHeight(PANEL_HEIGHT);
        background.setPosition(panelX, panelY);
        furnacePanel.attachChild(background);
        
        // Input slot (top left)
        createSlotVisual(0, panelX + 50, panelY + 150, furnace.getInputSlot());
        
        // Fuel slot (bottom left)
        createSlotVisual(1, panelX + 50, panelY + 80, furnace.getFuelSlot());
        
        // Output slot (right)
        createSlotVisual(2, panelX + 200, panelY + 115, furnace.getOutputSlot());
        
        // Progress bars
        createProgressBars(panelX, panelY);
        
        guiNode.attachChild(furnacePanel);
    }
    
    private void createSlotVisual(int slotIndex, int x, int y, ItemStack stack) {
        Picture slot = new Picture("FurnaceSlot_" + slotIndex);
        slot.setImage(app.getAssetManager(), "Textures/UI/slot.png", true);
        slot.setWidth(SLOT_SIZE);
        slot.setHeight(SLOT_SIZE);
        slot.setPosition(x, y);
        furnacePanel.attachChild(slot);
        
        // Item icon (if slot has item)
        if (!stack.isEmpty()) {
            createItemIcon(slotIndex, stack, x, y);
        }
    }
    
    private void createItemIcon(int slotIndex, ItemStack stack, int x, int y) {
        ItemDefinition item = stack.getItem();
        Picture icon = new Picture("FurnaceItemIcon_" + slotIndex);
        icon.setImage(app.getAssetManager(), "Textures/Items/" + item.getIcon(), true);
        icon.setWidth(SLOT_SIZE - 4);
        icon.setHeight(SLOT_SIZE - 4);
        icon.setPosition(x + 2, y + 2);
        furnacePanel.attachChild(icon);
        
        // Add stack count text overlay
        if (stack.getCount() > 1) {
            BitmapText countText = new BitmapText(font);
            countText.setText(String.valueOf(stack.getCount()));
            countText.setSize(font.getCharSet().getRenderedSize());
            countText.setColor(ColorRGBA.White);
            
            // Position at bottom-right with shadow
            float textWidth = countText.getLineWidth();
            countText.setLocalTranslation(x + SLOT_SIZE - textWidth - 4, y + 12, 0);
            
            // Add shadow
            BitmapText shadowText = new BitmapText(font);
            shadowText.setText(String.valueOf(stack.getCount()));
            shadowText.setSize(font.getCharSet().getRenderedSize());
            shadowText.setColor(new ColorRGBA(0, 0, 0, 0.5f));
            shadowText.setLocalTranslation(x + SLOT_SIZE - textWidth - 3, y + 11, -0.1f);
            
            furnacePanel.attachChild(shadowText);
            furnacePanel.attachChild(countText);
        }
    }
    
    private void createProgressBars(int panelX, int panelY) {
        // Burn progress (fire icon)
        if (furnace.getBurnTime() > 0) {
            int burnHeight = (int) ((float) furnace.getBurnTime() / furnace.getBurnTimeMax() * 14);
            Picture fire = new Picture("FurnaceFire");
            fire.setImage(app.getAssetManager(), "Textures/UI/fire.png", true);
            fire.setWidth(14);
            fire.setHeight(burnHeight);
            fire.setPosition(panelX + 60, panelY + 80 - burnHeight);
            furnacePanel.attachChild(fire);
        }
        
        // Smelt progress (arrow)
        if (furnace.getSmeltTime() > 0) {
            int arrowWidth = (int) ((float) furnace.getSmeltTime() / furnace.getSmeltTimeMax() * 24);
            Picture arrow = new Picture("FurnaceArrow");
            arrow.setImage(app.getAssetManager(), "Textures/UI/arrow.png", true);
            arrow.setWidth(arrowWidth);
            arrow.setHeight(16);
            arrow.setPosition(panelX + 120, panelY + 120);
            furnacePanel.attachChild(arrow);
        }
    }
    
    @Override
    public void update(float tpf) {
        if (!isEnabled()) {
            return;
        }
        
        // Update hovered slot based on mouse position
        Vector2f cursorPos = app.getInputManager().getCursorPosition();
        hoveredSlot = getSlotAtPosition((int) cursorPos.x, (int) cursorPos.y);
        
        // Refresh UI to show progress bars
        refreshUI();
    }
    
    private int getSlotAtPosition(int mouseX, int mouseY) {
        int screenWidth = app.getCamera().getWidth();
        int screenHeight = app.getCamera().getHeight();
        int panelX = (screenWidth - PANEL_WIDTH) / 2;
        int panelY = (screenHeight - PANEL_HEIGHT) / 2;
        
        // Input slot
        if (isInSlot(mouseX, mouseY, panelX + 50, panelY + 150)) {
            return 0;
        }
        
        // Fuel slot
        if (isInSlot(mouseX, mouseY, panelX + 50, panelY + 80)) {
            return 1;
        }
        
        // Output slot
        if (isInSlot(mouseX, mouseY, panelX + 200, panelY + 115)) {
            return 2;
        }
        
        return -1;
    }
    
    private boolean isInSlot(int mouseX, int mouseY, int slotX, int slotY) {
        return mouseX >= slotX && mouseX < slotX + SLOT_SIZE &&
               mouseY >= slotY && mouseY < slotY + SLOT_SIZE;
    }
    
    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!isPressed) {
            return;
        }
        
        switch (name) {
            case "CloseFurnace":
                close();
                break;
            case "FurnaceClick":
                handleLeftClick();
                break;
            case "FurnaceRightClick":
                handleRightClick();
                break;
        }
    }
    
    private void handleLeftClick() {
        if (hoveredSlot < 0) {
            return;
        }
        
        ItemStack slotStack = getSlotStack(hoveredSlot);
        
        if (heldStack.isEmpty() && !slotStack.isEmpty()) {
            // Pick up entire stack
            if (hoveredSlot == 2) {
                // Output slot - can only take out
                heldStack = slotStack.copy();
                setSlotStack(hoveredSlot, ItemStack.empty());
            } else {
                heldStack = slotStack.copy();
                setSlotStack(hoveredSlot, ItemStack.empty());
            }
        } else if (!heldStack.isEmpty() && slotStack.isEmpty()) {
            // Place entire held stack
            if (hoveredSlot == 2) {
                // Can't place in output slot
                return;
            }
            if (hoveredSlot == 1 && !FuelRegistry.getInstance().isFuel(heldStack.getItemId())) {
                // Can't place non-fuel in fuel slot
                return;
            }
            setSlotStack(hoveredSlot, heldStack);
            heldStack = ItemStack.empty();
        } else if (!heldStack.isEmpty() && !slotStack.isEmpty()) {
            if (hoveredSlot == 2) {
                // Output slot - swap if compatible
                if (heldStack.canMergeWith(slotStack)) {
                    int spaceLeft = heldStack.getMaxStackSize() - heldStack.getCount();
                    int toTransfer = Math.min(spaceLeft, slotStack.getCount());
                    heldStack = heldStack.grow(toTransfer);
                    setSlotStack(hoveredSlot, slotStack.shrink(toTransfer));
                }
            } else {
                // Swap stacks
                ItemStack temp = heldStack;
                heldStack = slotStack;
                setSlotStack(hoveredSlot, temp);
            }
        }
        
        refreshUI();
    }
    
    private void handleRightClick() {
        if (hoveredSlot < 0) {
            return;
        }
        
        ItemStack slotStack = getSlotStack(hoveredSlot);
        
        if (heldStack.isEmpty() && !slotStack.isEmpty()) {
            // Pick up half stack
            int halfCount = (slotStack.getCount() + 1) / 2;
            heldStack = slotStack.split(halfCount);
            setSlotStack(hoveredSlot, slotStack.shrink(halfCount));
        } else if (!heldStack.isEmpty() && slotStack.isEmpty()) {
            // Place single item
            if (hoveredSlot == 2) {
                return; // Can't place in output
            }
            if (hoveredSlot == 1 && !FuelRegistry.getInstance().isFuel(heldStack.getItemId())) {
                return; // Can't place non-fuel in fuel slot
            }
            setSlotStack(hoveredSlot, heldStack.split(1));
            heldStack = heldStack.shrink(1);
        } else if (!heldStack.isEmpty() && !slotStack.isEmpty() && heldStack.canMergeWith(slotStack)) {
            // Add single item to stack
            if (hoveredSlot == 2) {
                // Take one from output
                if (heldStack.getCount() < heldStack.getMaxStackSize()) {
                    heldStack = heldStack.grow(1);
                    setSlotStack(hoveredSlot, slotStack.shrink(1));
                }
            } else {
                // Add one to slot
                if (slotStack.getCount() < slotStack.getMaxStackSize()) {
                    setSlotStack(hoveredSlot, slotStack.grow(1));
                    heldStack = heldStack.shrink(1);
                }
            }
        }
        
        refreshUI();
    }
    
    private ItemStack getSlotStack(int slot) {
        switch (slot) {
            case 0: return furnace.getInputSlot();
            case 1: return furnace.getFuelSlot();
            case 2: return furnace.getOutputSlot();
            default: return ItemStack.empty();
        }
    }
    
    private void setSlotStack(int slot, ItemStack stack) {
        switch (slot) {
            case 0: furnace.setInputSlot(stack); break;
            case 1: furnace.setFuelSlot(stack); break;
            case 2: furnace.setOutputSlot(stack); break;
        }
    }
    
    private void refreshUI() {
        // Remove old UI
        furnacePanel.detachAllChildren();
        guiNode.detachChild(furnacePanel);
        
        // Recreate UI
        createFurnaceUI();
    }
    
    private void close() {
        // Return held stack to inventory
        if (!heldStack.isEmpty()) {
            ItemStack remaining = playerInventory.addItem(heldStack);
            if (!remaining.isEmpty()) {
                logger.warn("Dropped {} items on furnace close", remaining.getCount());
            }
        }
        
        stateManager.detach(this);
    }
    
    @Override
    public void cleanup() {
        super.cleanup();
        
        // Remove UI
        if (furnacePanel != null) {
            guiNode.detachChild(furnacePanel);
        }
        
        // Re-enable FirstPersonController input
        if (controller != null) {
            controller.setInputEnabled(true);
        }
        
        // Hide cursor
        app.getInputManager().setCursorVisible(false);
        
        // Remove input mappings
        app.getInputManager().removeListener(this);
        app.getInputManager().deleteMapping("CloseFurnace");
        app.getInputManager().deleteMapping("FurnaceClick");
        app.getInputManager().deleteMapping("FurnaceRightClick");
        
        logger.info("FurnaceAppState cleaned up");
    }
    
    public FurnaceBlockEntity getFurnace() {
        return furnace;
    }
}
