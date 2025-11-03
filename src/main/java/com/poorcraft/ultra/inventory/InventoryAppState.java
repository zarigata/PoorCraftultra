package com.poorcraft.ultra.inventory;

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
import com.poorcraft.ultra.crafting.CraftingGrid;
import com.poorcraft.ultra.crafting.CraftingListener;
import com.poorcraft.ultra.items.ItemDefinition;
import com.poorcraft.ultra.player.FirstPersonController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Player inventory UI (CP v3.1).
 * Displays hotbar, main inventory, and 2x2 crafting grid.
 */
public class InventoryAppState extends AbstractAppState implements ActionListener {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryAppState.class);
    
    private SimpleApplication app;
    private AppStateManager stateManager;
    private Node guiNode;
    private PlayerInventory inventory;
    private FirstPersonController controller;
    
    private Node inventoryPanel;
    private Node tooltipNode;
    private ItemStack heldStack = ItemStack.empty();
    private int hoveredSlot = -1;
    private int hoveredCraftingSlot = -1; // -1=none, 0-3=crafting slots, 4=result
    private CraftingGrid craftingGrid;
    private BitmapFont font;
    private boolean shiftPressed = false;
    
    // Layout constants
    private static final int SLOT_SIZE = 40;
    private static final int SLOT_PADDING = 4;
    private static final int PANEL_WIDTH = 9 * (SLOT_SIZE + SLOT_PADDING) + 40;
    private static final int PANEL_HEIGHT = 6 * (SLOT_SIZE + SLOT_PADDING) + 80;
    private static final int CRAFTING_GRID_X_OFFSET = 20;
    private static final int CRAFTING_GRID_Y_OFFSET = 200;
    
    public InventoryAppState(PlayerInventory inventory, FirstPersonController controller) {
        this.inventory = inventory;
        this.controller = controller;
    }
    
    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (SimpleApplication) app;
        this.stateManager = stateManager;
        this.guiNode = this.app.getGuiNode();
        
        logger.info("Initializing InventoryAppState...");
        
        // Initialize crafting grid
        craftingGrid = new CraftingGrid(2, 2);
        craftingGrid.addListener(new CraftingListener() {
            @Override
            public void onResultChanged(ItemStack newResult) {
                refreshUI();
            }
        });
        
        // Load font
        font = this.app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        
        // Disable FirstPersonController input
        if (controller != null) {
            controller.setInputEnabled(false);
        }
        
        // Show cursor
        app.getInputManager().setCursorVisible(true);
        
        // Create UI
        createInventoryUI();
        
        // Set up input
        app.getInputManager().addMapping("CloseInventory", new KeyTrigger(KeyInput.KEY_E));
        app.getInputManager().addMapping("CloseInventory", new KeyTrigger(KeyInput.KEY_ESCAPE));
        app.getInputManager().addMapping("InventoryClick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        app.getInputManager().addMapping("InventoryRightClick", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        app.getInputManager().addMapping("ShiftKey", new KeyTrigger(KeyInput.KEY_LSHIFT));
        app.getInputManager().addMapping("ShiftKey", new KeyTrigger(KeyInput.KEY_RSHIFT));
        app.getInputManager().addListener(this, "CloseInventory", "InventoryClick", "InventoryRightClick", "ShiftKey");
        
        logger.info("InventoryAppState initialized");
    }
    
    private void createInventoryUI() {
        inventoryPanel = new Node("InventoryPanel");
        
        int screenWidth = app.getCamera().getWidth();
        int screenHeight = app.getCamera().getHeight();
        int panelX = (screenWidth - PANEL_WIDTH) / 2;
        int panelY = (screenHeight - PANEL_HEIGHT) / 2;
        
        // Background panel
        Picture background = new Picture("InventoryBackground");
        background.setImage(app.getAssetManager(), "Textures/UI/inventory_bg.png", true);
        background.setWidth(PANEL_WIDTH);
        background.setHeight(PANEL_HEIGHT);
        background.setPosition(panelX, panelY);
        inventoryPanel.attachChild(background);
        
        // Create slot visuals (hotbar + main inventory)
        int slotIndex = 0;
        
        // Hotbar (bottom row)
        for (int i = 0; i < 9; i++) {
            createSlotVisual(slotIndex++, panelX + 20 + i * (SLOT_SIZE + SLOT_PADDING), 
                           panelY + 20);
        }
        
        // Main inventory (3 rows above hotbar)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                createSlotVisual(slotIndex++, 
                               panelX + 20 + col * (SLOT_SIZE + SLOT_PADDING),
                               panelY + 80 + row * (SLOT_SIZE + SLOT_PADDING));
            }
        }
        
        // 2x2 crafting grid (top left)
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                int craftingSlotIndex = row * 2 + col;
                createCraftingSlotVisual(craftingSlotIndex, 
                                        panelX + CRAFTING_GRID_X_OFFSET + col * (SLOT_SIZE + SLOT_PADDING),
                                        panelY + CRAFTING_GRID_Y_OFFSET + row * (SLOT_SIZE + SLOT_PADDING));
            }
        }
        
        // Crafting result slot (right of crafting grid)
        createCraftingResultSlotVisual(panelX + CRAFTING_GRID_X_OFFSET + 3 * (SLOT_SIZE + SLOT_PADDING),
                                      panelY + CRAFTING_GRID_Y_OFFSET + (SLOT_SIZE + SLOT_PADDING) / 2);
        
        guiNode.attachChild(inventoryPanel);
    }
    
    private void createSlotVisual(int slotIndex, int x, int y) {
        Picture slot = new Picture("Slot_" + slotIndex);
        slot.setImage(app.getAssetManager(), "Textures/UI/slot.png", true);
        slot.setWidth(SLOT_SIZE);
        slot.setHeight(SLOT_SIZE);
        slot.setPosition(x, y);
        inventoryPanel.attachChild(slot);
        
        // Item icon (if slot has item)
        ItemStack stack = inventory.getStack(slotIndex);
        if (!stack.isEmpty()) {
            createItemIcon(slotIndex, stack, x, y);
        }
    }
    
    private void createItemIcon(int slotIndex, ItemStack stack, int x, int y) {
        ItemDefinition item = stack.getItem();
        Picture icon = new Picture("ItemIcon_" + slotIndex);
        icon.setImage(app.getAssetManager(), "Textures/Items/" + item.getIcon(), true);
        icon.setWidth(SLOT_SIZE - 4);
        icon.setHeight(SLOT_SIZE - 4);
        icon.setPosition(x + 2, y + 2);
        inventoryPanel.attachChild(icon);
        
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
            
            inventoryPanel.attachChild(shadowText);
            inventoryPanel.attachChild(countText);
        }
    }
    
    private void createCraftingSlotVisual(int slotIndex, int x, int y) {
        Picture slot = new Picture("CraftingSlot_" + slotIndex);
        slot.setImage(app.getAssetManager(), "Textures/UI/slot.png", true);
        slot.setWidth(SLOT_SIZE);
        slot.setHeight(SLOT_SIZE);
        slot.setPosition(x, y);
        inventoryPanel.attachChild(slot);
        
        // Item icon (if slot has item)
        ItemStack stack = craftingGrid.getSlot(slotIndex);
        if (!stack.isEmpty()) {
            createCraftingItemIcon(slotIndex, stack, x, y);
        }
    }
    
    private void createCraftingResultSlotVisual(int x, int y) {
        Picture slot = new Picture("CraftingResultSlot");
        slot.setImage(app.getAssetManager(), "Textures/UI/slot.png", true);
        slot.setWidth(SLOT_SIZE);
        slot.setHeight(SLOT_SIZE);
        slot.setPosition(x, y);
        inventoryPanel.attachChild(slot);
        
        // Item icon (if result exists)
        ItemStack resultStack = craftingGrid.getResultSlot();
        if (!resultStack.isEmpty()) {
            ItemDefinition item = resultStack.getItem();
            Picture icon = new Picture("CraftingResultIcon");
            icon.setImage(app.getAssetManager(), "Textures/Items/" + item.getIcon(), true);
            icon.setWidth(SLOT_SIZE - 4);
            icon.setHeight(SLOT_SIZE - 4);
            icon.setPosition(x + 2, y + 2);
            inventoryPanel.attachChild(icon);
            
            // Add stack count
            if (resultStack.getCount() > 1) {
                BitmapText countText = new BitmapText(font);
                countText.setText(String.valueOf(resultStack.getCount()));
                countText.setSize(font.getCharSet().getRenderedSize());
                countText.setColor(ColorRGBA.White);
                
                float textWidth = countText.getLineWidth();
                countText.setLocalTranslation(x + SLOT_SIZE - textWidth - 4, y + 12, 0);
                
                BitmapText shadowText = new BitmapText(font);
                shadowText.setText(String.valueOf(resultStack.getCount()));
                shadowText.setSize(font.getCharSet().getRenderedSize());
                shadowText.setColor(new ColorRGBA(0, 0, 0, 0.5f));
                shadowText.setLocalTranslation(x + SLOT_SIZE - textWidth - 3, y + 11, -0.1f);
                
                inventoryPanel.attachChild(shadowText);
                inventoryPanel.attachChild(countText);
            }
        }
    }
    
    private void createCraftingItemIcon(int slotIndex, ItemStack stack, int x, int y) {
        ItemDefinition item = stack.getItem();
        Picture icon = new Picture("CraftingItemIcon_" + slotIndex);
        icon.setImage(app.getAssetManager(), "Textures/Items/" + item.getIcon(), true);
        icon.setWidth(SLOT_SIZE - 4);
        icon.setHeight(SLOT_SIZE - 4);
        icon.setPosition(x + 2, y + 2);
        inventoryPanel.attachChild(icon);
        
        // Add stack count
        if (stack.getCount() > 1) {
            BitmapText countText = new BitmapText(font);
            countText.setText(String.valueOf(stack.getCount()));
            countText.setSize(font.getCharSet().getRenderedSize());
            countText.setColor(ColorRGBA.White);
            
            float textWidth = countText.getLineWidth();
            countText.setLocalTranslation(x + SLOT_SIZE - textWidth - 4, y + 12, 0);
            
            BitmapText shadowText = new BitmapText(font);
            shadowText.setText(String.valueOf(stack.getCount()));
            shadowText.setSize(font.getCharSet().getRenderedSize());
            shadowText.setColor(new ColorRGBA(0, 0, 0, 0.5f));
            shadowText.setLocalTranslation(x + SLOT_SIZE - textWidth - 3, y + 11, -0.1f);
            
            inventoryPanel.attachChild(shadowText);
            inventoryPanel.attachChild(countText);
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
        hoveredCraftingSlot = getCraftingSlotAtPosition((int) cursorPos.x, (int) cursorPos.y);
        
        // Update tooltip for hovered item
        updateTooltip((int) cursorPos.x, (int) cursorPos.y);
    }
    
    private int getSlotAtPosition(int mouseX, int mouseY) {
        int screenWidth = app.getCamera().getWidth();
        int screenHeight = app.getCamera().getHeight();
        int panelX = (screenWidth - PANEL_WIDTH) / 2;
        int panelY = (screenHeight - PANEL_HEIGHT) / 2;
        
        // Check hotbar
        for (int i = 0; i < 9; i++) {
            int slotX = panelX + 20 + i * (SLOT_SIZE + SLOT_PADDING);
            int slotY = panelY + 20;
            if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE &&
                mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                return i;
            }
        }
        
        // Check main inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = panelX + 20 + col * (SLOT_SIZE + SLOT_PADDING);
                int slotY = panelY + 80 + row * (SLOT_SIZE + SLOT_PADDING);
                if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE &&
                    mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                    return 9 + row * 9 + col;
                }
            }
        }
        
        return -1;
    }
    
    private int getCraftingSlotAtPosition(int mouseX, int mouseY) {
        int screenWidth = app.getCamera().getWidth();
        int screenHeight = app.getCamera().getHeight();
        int panelX = (screenWidth - PANEL_WIDTH) / 2;
        int panelY = (screenHeight - PANEL_HEIGHT) / 2;
        
        // Check crafting grid (2x2)
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                int slotX = panelX + CRAFTING_GRID_X_OFFSET + col * (SLOT_SIZE + SLOT_PADDING);
                int slotY = panelY + CRAFTING_GRID_Y_OFFSET + row * (SLOT_SIZE + SLOT_PADDING);
                if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE &&
                    mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                    return row * 2 + col;
                }
            }
        }
        
        // Check result slot
        int resultX = panelX + CRAFTING_GRID_X_OFFSET + 3 * (SLOT_SIZE + SLOT_PADDING);
        int resultY = panelY + CRAFTING_GRID_Y_OFFSET + (SLOT_SIZE + SLOT_PADDING) / 2;
        if (mouseX >= resultX && mouseX < resultX + SLOT_SIZE &&
            mouseY >= resultY && mouseY < resultY + SLOT_SIZE) {
            return 4; // Result slot
        }
        
        return -1;
    }
    
    private void updateTooltip(int mouseX, int mouseY) {
        // Remove old tooltip
        if (tooltipNode != null) {
            guiNode.detachChild(tooltipNode);
            tooltipNode = null;
        }
        
        // Don't show tooltip if holding an item
        if (!heldStack.isEmpty()) {
            return;
        }
        
        ItemStack hoveredStack = null;
        
        // Check inventory slots
        if (hoveredSlot >= 0 && hoveredSlot < 36) {
            hoveredStack = inventory.getStack(hoveredSlot);
        }
        
        // Check crafting slots
        if (hoveredCraftingSlot >= 0 && hoveredCraftingSlot < 4) {
            hoveredStack = craftingGrid.getSlot(hoveredCraftingSlot);
        } else if (hoveredCraftingSlot == 4) {
            hoveredStack = craftingGrid.getResultSlot();
        }
        
        if (hoveredStack != null && !hoveredStack.isEmpty()) {
            createTooltip(hoveredStack, mouseX, mouseY);
        }
    }
    
    private void createTooltip(ItemStack stack, int mouseX, int mouseY) {
        tooltipNode = new Node("Tooltip");
        
        ItemDefinition item = stack.getItem();
        String displayName = item.getDisplayName();
        
        // Build tooltip text
        StringBuilder tooltipText = new StringBuilder(displayName);
        if (item.isTool() && stack.getDurability() >= 0) {
            tooltipText.append("\n").append(stack.getDurability()).append("/").append(item.getDurability());
        }
        
        // Create text
        BitmapText text = new BitmapText(font);
        text.setText(tooltipText.toString());
        text.setSize(font.getCharSet().getRenderedSize());
        text.setColor(ColorRGBA.White);
        
        // Calculate tooltip size
        float textWidth = text.getLineWidth();
        float textHeight = text.getLineHeight() * (tooltipText.toString().split("\n").length);
        float padding = 4;
        float tooltipWidth = textWidth + padding * 2;
        float tooltipHeight = textHeight + padding * 2;
        
        // Position tooltip near cursor
        float tooltipX = mouseX + 10;
        float tooltipY = mouseY + 10;
        
        // Keep tooltip on screen
        if (tooltipX + tooltipWidth > app.getCamera().getWidth()) {
            tooltipX = mouseX - tooltipWidth - 10;
        }
        if (tooltipY + tooltipHeight > app.getCamera().getHeight()) {
            tooltipY = mouseY - tooltipHeight - 10;
        }
        
        // Background
        Picture background = new Picture("TooltipBackground");
        background.setImage(app.getAssetManager(), "Common/Util/UnitQuad.png", true);
        background.setWidth(tooltipWidth);
        background.setHeight(tooltipHeight);
        background.setPosition(tooltipX, tooltipY);
        background.getMaterial().setColor("Color", new ColorRGBA(0, 0, 0, 0.8f));
        tooltipNode.attachChild(background);
        
        // Text
        text.setLocalTranslation(tooltipX + padding, tooltipY + tooltipHeight - padding, 1);
        tooltipNode.attachChild(text);
        
        guiNode.attachChild(tooltipNode);
    }
    
    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        switch (name) {
            case "ShiftKey":
                shiftPressed = isPressed;
                return;
            case "CloseInventory":
                if (isPressed) close();
                break;
            case "InventoryClick":
                if (isPressed) handleLeftClick();
                break;
            case "InventoryRightClick":
                if (isPressed) handleRightClick();
                break;
        }
    }
    
    private void handleLeftClick() {
        // Handle crafting slots first
        if (hoveredCraftingSlot >= 0) {
            handleCraftingClick(shiftPressed);
            return;
        }
        
        if (hoveredSlot < 0 || hoveredSlot >= 36) {
            return;
        }
        
        // Shift-click quick transfer
        if (shiftPressed && heldStack.isEmpty()) {
            handleShiftClick();
            return;
        }
        
        ItemStack slotStack = inventory.getStack(hoveredSlot);
        
        if (heldStack.isEmpty() && !slotStack.isEmpty()) {
            // Pick up entire stack
            heldStack = slotStack.copy();
            inventory.setStack(hoveredSlot, ItemStack.empty());
        } else if (!heldStack.isEmpty() && slotStack.isEmpty()) {
            // Place entire held stack
            inventory.setStack(hoveredSlot, heldStack);
            heldStack = ItemStack.empty();
        } else if (!heldStack.isEmpty() && !slotStack.isEmpty()) {
            if (heldStack.canMergeWith(slotStack)) {
                // Merge stacks
                int spaceLeft = slotStack.getMaxStackSize() - slotStack.getCount();
                int toTransfer = Math.min(spaceLeft, heldStack.getCount());
                inventory.setStack(hoveredSlot, slotStack.grow(toTransfer));
                heldStack = heldStack.shrink(toTransfer);
            } else {
                // Swap stacks
                ItemStack temp = heldStack;
                heldStack = slotStack;
                inventory.setStack(hoveredSlot, temp);
            }
        }
        
        refreshUI();
    }
    
    private void handleShiftClick() {
        ItemStack slotStack = inventory.getStack(hoveredSlot);
        if (slotStack.isEmpty()) {
            return;
        }
        
        // Determine target range
        int targetStart, targetEnd;
        if (hoveredSlot < 9) {
            // Hotbar -> Main inventory (slots 9-35)
            targetStart = 9;
            targetEnd = 35;
        } else {
            // Main inventory -> Hotbar (slots 0-8)
            targetStart = 0;
            targetEnd = 8;
        }
        
        // Try to merge with existing stacks first
        ItemStack remaining = slotStack.copy();
        for (int i = targetStart; i <= targetEnd && !remaining.isEmpty(); i++) {
            ItemStack targetStack = inventory.getStack(i);
            if (!targetStack.isEmpty() && targetStack.canMergeWith(remaining)) {
                int spaceLeft = targetStack.getMaxStackSize() - targetStack.getCount();
                if (spaceLeft > 0) {
                    int toTransfer = Math.min(spaceLeft, remaining.getCount());
                    inventory.setStack(i, targetStack.grow(toTransfer));
                    remaining = remaining.shrink(toTransfer);
                }
            }
        }
        
        // Fill empty slots
        for (int i = targetStart; i <= targetEnd && !remaining.isEmpty(); i++) {
            if (inventory.getStack(i).isEmpty()) {
                inventory.setStack(i, remaining);
                remaining = ItemStack.empty();
            }
        }
        
        // Update source slot
        inventory.setStack(hoveredSlot, remaining);
        refreshUI();
    }
    
    private void handleCraftingClick(boolean shiftPressed) {
        if (hoveredCraftingSlot == 4) {
            // Result slot - craft item
            ItemStack result = craftingGrid.getResultSlot();
            if (result.isEmpty()) {
                return;
            }
            
            if (heldStack.isEmpty()) {
                // Take result
                ItemStack crafted = craftingGrid.craft();
                if (shiftPressed) {
                    // Shift-click: add to inventory
                    ItemStack remaining = inventory.addItem(crafted);
                    if (!remaining.isEmpty()) {
                        heldStack = remaining;
                    }
                } else {
                    heldStack = crafted;
                }
            } else if (heldStack.canMergeWith(result)) {
                // Merge with held stack
                int spaceLeft = heldStack.getMaxStackSize() - heldStack.getCount();
                if (spaceLeft >= result.getCount()) {
                    ItemStack crafted = craftingGrid.craft();
                    heldStack = heldStack.grow(crafted.getCount());
                }
            }
        } else if (hoveredCraftingSlot >= 0 && hoveredCraftingSlot < 4) {
            // Crafting input slot
            ItemStack slotStack = craftingGrid.getSlot(hoveredCraftingSlot);
            
            if (heldStack.isEmpty() && !slotStack.isEmpty()) {
                // Pick up entire stack
                heldStack = slotStack.copy();
                craftingGrid.setSlot(hoveredCraftingSlot, ItemStack.empty());
            } else if (!heldStack.isEmpty() && slotStack.isEmpty()) {
                // Place entire held stack
                craftingGrid.setSlot(hoveredCraftingSlot, heldStack);
                heldStack = ItemStack.empty();
            } else if (!heldStack.isEmpty() && !slotStack.isEmpty()) {
                if (heldStack.canMergeWith(slotStack)) {
                    // Merge stacks
                    int spaceLeft = slotStack.getMaxStackSize() - slotStack.getCount();
                    int toTransfer = Math.min(spaceLeft, heldStack.getCount());
                    craftingGrid.setSlot(hoveredCraftingSlot, slotStack.grow(toTransfer));
                    heldStack = heldStack.shrink(toTransfer);
                } else {
                    // Swap stacks
                    ItemStack temp = heldStack;
                    heldStack = slotStack;
                    craftingGrid.setSlot(hoveredCraftingSlot, temp);
                }
            }
        }
        
        refreshUI();
    }
    
    private void handleRightClick() {
        // Handle crafting slots first
        if (hoveredCraftingSlot >= 0 && hoveredCraftingSlot < 4) {
            handleCraftingRightClick();
            return;
        }
        
        if (hoveredSlot < 0 || hoveredSlot >= 36) {
            return;
        }
        
        ItemStack slotStack = inventory.getStack(hoveredSlot);
        
        if (heldStack.isEmpty() && !slotStack.isEmpty()) {
            // Pick up half stack
            int halfCount = (slotStack.getCount() + 1) / 2;
            heldStack = slotStack.split(halfCount);
            inventory.setStack(hoveredSlot, slotStack.shrink(halfCount));
        } else if (!heldStack.isEmpty() && slotStack.isEmpty()) {
            // Place single item
            inventory.setStack(hoveredSlot, heldStack.split(1));
            heldStack = heldStack.shrink(1);
        } else if (!heldStack.isEmpty() && !slotStack.isEmpty() && heldStack.canMergeWith(slotStack)) {
            // Add single item to stack
            if (slotStack.getCount() < slotStack.getMaxStackSize()) {
                inventory.setStack(hoveredSlot, slotStack.grow(1));
                heldStack = heldStack.shrink(1);
            }
        }
        
        refreshUI();
    }
    
    private void handleCraftingRightClick() {
        ItemStack slotStack = craftingGrid.getSlot(hoveredCraftingSlot);
        
        if (heldStack.isEmpty() && !slotStack.isEmpty()) {
            // Pick up half stack
            int halfCount = (slotStack.getCount() + 1) / 2;
            heldStack = slotStack.split(halfCount);
            craftingGrid.setSlot(hoveredCraftingSlot, slotStack.shrink(halfCount));
        } else if (!heldStack.isEmpty() && slotStack.isEmpty()) {
            // Place single item
            craftingGrid.setSlot(hoveredCraftingSlot, heldStack.split(1));
            heldStack = heldStack.shrink(1);
        } else if (!heldStack.isEmpty() && !slotStack.isEmpty() && heldStack.canMergeWith(slotStack)) {
            // Add single item to stack
            if (slotStack.getCount() < slotStack.getMaxStackSize()) {
                craftingGrid.setSlot(hoveredCraftingSlot, slotStack.grow(1));
                heldStack = heldStack.shrink(1);
            }
        }
        
        refreshUI();
    }
    
    private void refreshUI() {
        // Remove old UI
        inventoryPanel.detachAllChildren();
        guiNode.detachChild(inventoryPanel);
        
        // Recreate UI
        createInventoryUI();
    }
    
    private void close() {
        // Return held stack to inventory
        if (!heldStack.isEmpty()) {
            ItemStack remaining = inventory.addItem(heldStack);
            if (!remaining.isEmpty()) {
                logger.warn("Dropped {} items on inventory close", remaining.getCount());
            }
        }
        
        // Return crafting grid items to inventory
        for (int i = 0; i < 4; i++) {
            ItemStack craftingStack = craftingGrid.getSlot(i);
            if (!craftingStack.isEmpty()) {
                ItemStack remaining = inventory.addItem(craftingStack);
                if (!remaining.isEmpty()) {
                    logger.warn("Dropped {} crafting items on inventory close", remaining.getCount());
                }
            }
        }
        
        stateManager.detach(this);
    }
    
    @Override
    public void cleanup() {
        super.cleanup();
        
        // Remove UI
        if (inventoryPanel != null) {
            guiNode.detachChild(inventoryPanel);
        }
        if (tooltipNode != null) {
            guiNode.detachChild(tooltipNode);
        }
        
        // Re-enable FirstPersonController input
        if (controller != null) {
            controller.setInputEnabled(true);
        }
        
        // Hide cursor
        app.getInputManager().setCursorVisible(false);
        
        // Remove input mappings
        app.getInputManager().removeListener(this);
        app.getInputManager().deleteMapping("CloseInventory");
        app.getInputManager().deleteMapping("InventoryClick");
        app.getInputManager().deleteMapping("InventoryRightClick");
        
        logger.info("InventoryAppState cleaned up");
    }
}
