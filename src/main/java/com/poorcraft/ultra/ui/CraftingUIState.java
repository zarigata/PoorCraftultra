package com.poorcraft.ultra.ui;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.InputManager;
import com.jme3.math.Vector3f;
import com.poorcraft.ultra.app.ServiceHub;
import com.poorcraft.ultra.gameplay.CraftingManager;
import com.poorcraft.ultra.gameplay.CraftingRecipe;
import com.poorcraft.ultra.gameplay.ItemStack;
import com.poorcraft.ultra.player.PlayerController;
import com.poorcraft.ultra.player.PlayerInventory;
import com.poorcraft.ultra.voxel.BlockType;
import com.simsilica.lemur.Axis;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.style.BaseStyles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Lemur-based crafting overlay for 2x2 shapeless recipes.
 */
public class CraftingUIState extends BaseAppState {

    private static final Logger logger = LoggerFactory.getLogger(CraftingUIState.class);

    private final ServiceHub serviceHub;

    private SimpleApplication application;
    private PlayerInventory inventory;
    private CraftingManager craftingManager;
    private InputConfig inputConfig;
    private PlayerController playerController;

    private Container rootContainer;
    private final Button[] slotButtons = new Button[4];
    private final BlockType[] slotContents = new BlockType[4];
    private Label outputLabel;
    private Label statusLabel;
    private Button craftButton;

    private boolean restoreCursorVisible;
    private boolean restorePlayerInputs;

    private final com.jme3.input.controls.ActionListener craftHotkeyListener = (name, isPressed, tpf) -> {
        if (!"craft".equals(name) || isPressed) {
            return;
        }
        toggle();
    };

    public CraftingUIState(ServiceHub serviceHub) {
        this.serviceHub = Objects.requireNonNull(serviceHub, "serviceHub");
        for (int i = 0; i < slotContents.length; i++) {
            slotContents[i] = BlockType.AIR;
        }
    }

    public void toggle() {
        setEnabled(!isEnabled());
    }

    @Override
    protected void initialize(Application app) {
        application = (SimpleApplication) app;
        inventory = serviceHub.get(PlayerInventory.class);
        craftingManager = serviceHub.get(CraftingManager.class);
        inputConfig = serviceHub.get(InputConfig.class);
        playerController = serviceHub.get(PlayerController.class);

        ensureLemurInitialized(application);
        buildUi();
        if (inputConfig != null) {
            inputConfig.registerAction("craft", craftHotkeyListener);
        }
    }

    @Override
    protected void cleanup(Application app) {
        if (inputConfig != null) {
            inputConfig.unregisterAction("craft");
        }
        if (rootContainer != null) {
            rootContainer.removeFromParent();
        }
    }

    @Override
    protected void onEnable() {
        if (rootContainer != null && rootContainer.getParent() == null) {
            application.getGuiNode().attachChild(rootContainer);
        }
        clearGrid();
        updateLayout();
        updateOutput();
        statusLabel.setText("Select ingredients and press Craft");

        InputManager inputManager = application.getInputManager();
        if (inputManager != null) {
            restoreCursorVisible = inputManager.isCursorVisible();
            inputManager.setCursorVisible(true);
        }

        if (playerController != null) {
            restorePlayerInputs = playerController.isInputsEnabled();
            playerController.disable();
        }
    }

    @Override
    protected void onDisable() {
        if (rootContainer != null) {
            rootContainer.removeFromParent();
        }

        InputManager inputManager = application.getInputManager();
        if (inputManager != null) {
            inputManager.setCursorVisible(restoreCursorVisible);
        }

        if (playerController != null && restorePlayerInputs) {
            playerController.enable();
        }

        statusLabel.setText("");
    }

    private void ensureLemurInitialized(SimpleApplication app) {
        if (GuiGlobals.getInstance() == null) {
            GuiGlobals.initialize(app);
            BaseStyles.loadGlassStyle();
            GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");
            logger.info("Lemur GUI initialized for crafting UI");
        }
    }

    private void buildUi() {
        rootContainer = new Container();
        rootContainer.setPreferredSize(new Vector3f(320f, 260f, 0f));
        rootContainer.addChild(new Label("Crafting"));

        Container grid = rootContainer.addChild(new Container(new SpringGridLayout(Axis.Y, Axis.X)));
        for (int i = 0; i < slotButtons.length; i++) {
            final int index = i;
            Button button = grid.addChild(new Button(slotLabel(BlockType.AIR)));
            button.addClickCommands(source -> {
                cycleSlot(index);
                updateOutput();
            });
            slotButtons[i] = button;
        }

        outputLabel = rootContainer.addChild(new Label("?"));
        craftButton = rootContainer.addChild(new Button("Craft"));
        craftButton.addClickCommands(source -> attemptCraft());

        statusLabel = rootContainer.addChild(new Label(""));
        statusLabel.setTextHAlignment(com.simsilica.lemur.HAlignment.Center);

        updateLayout();
    }

    private void updateLayout() {
        if (application == null || rootContainer == null) {
            return;
        }
        float width = application.getCamera().getWidth();
        float height = application.getCamera().getHeight();
        Vector3f preferred = rootContainer.getPreferredSize();
        rootContainer.setLocalTranslation(
            width / 2f - preferred.x / 2f,
            height / 2f + preferred.y / 2f,
            0f
        );
    }

    private void cycleSlot(int index) {
        BlockType current = slotContents[index];
        List<BlockType> available = getAvailableItems();
        if (available.isEmpty()) {
            slotContents[index] = BlockType.AIR;
            slotButtons[index].setText(slotLabel(BlockType.AIR));
            return;
        }
        int startIndex = available.indexOf(current);
        int nextIndex = ((startIndex >= 0 ? startIndex : 0) + 1) % available.size();
        BlockType next = available.get(nextIndex);
        slotContents[index] = next;
        slotButtons[index].setText(slotLabel(next));
    }

    private List<BlockType> getAvailableItems() {
        List<BlockType> items = new ArrayList<>();
        items.add(BlockType.AIR);
        if (inventory != null) {
            for (BlockType type : BlockType.values()) {
                if (type == BlockType.AIR) {
                    continue;
                }
                if (inventory.getCount(type) > 0) {
                    items.add(type);
                }
            }
        }
        return items;
    }

    private void updateOutput() {
        List<BlockType> inputs = new ArrayList<>();
        for (BlockType type : slotContents) {
            if (type != null && type != BlockType.AIR) {
                inputs.add(type);
            }
        }

        CraftingRecipe recipe = craftingManager.findRecipe(inputs).orElse(null);
        if (recipe == null) {
            outputLabel.setText("?");
            craftButton.setEnabled(false);
        } else {
            outputLabel.setText(recipe.output().displayName() + " x" + recipe.outputCount());
            craftButton.setEnabled(true);
        }
    }

    private void attemptCraft() {
        List<BlockType> inputs = new ArrayList<>();
        for (BlockType type : slotContents) {
            if (type != null && type != BlockType.AIR) {
                inputs.add(type);
            }
        }

        if (inputs.isEmpty()) {
            statusLabel.setText("Select ingredients first");
            return;
        }

        ItemStack result = craftingManager.craft(inputs, inventory);
        if (result == null || result.isEmpty()) {
            statusLabel.setText("Missing ingredients");
            return;
        }

        clearGrid();
        updateOutput();
        statusLabel.setText("Crafted " + result.type().displayName() + " x" + result.count());
    }

    private void clearGrid() {
        for (int i = 0; i < slotContents.length; i++) {
            slotContents[i] = BlockType.AIR;
            slotButtons[i].setText(slotLabel(BlockType.AIR));
        }
    }

    private String slotLabel(BlockType type) {
        if (type == null || type == BlockType.AIR) {
            return "(empty)";
        }
        int count = inventory != null ? inventory.getCount(type) : 0;
        return type.displayName() + " (" + count + ")";
    }
}
