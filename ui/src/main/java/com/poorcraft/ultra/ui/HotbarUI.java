package com.poorcraft.ultra.ui;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import com.poorcraft.ultra.gameplay.Inventory;
import com.poorcraft.ultra.gameplay.InventoryListener;
import com.poorcraft.ultra.gameplay.ItemStack;
import com.poorcraft.ultra.shared.Logger;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.component.SpringGridLayout.Axis;
import com.simsilica.lemur.component.SpringGridLayout.FillMode;
import java.util.Objects;

public final class HotbarUI extends BaseAppState implements InventoryListener {

    private static final Logger logger = Logger.getLogger(HotbarUI.class);
    private static final float SLOT_SIZE = 50f;
    private static final float SLOT_PADDING = 4f;
    private static final float HOTBAR_Y_OFFSET = 80f;
    private static final ColorRGBA SELECTED_COLOR = new ColorRGBA(1f, 1f, 1f, 0.8f);
    private static final ColorRGBA UNSELECTED_COLOR = new ColorRGBA(0.3f, 0.3f, 0.3f, 0.6f);

    private final Inventory inventory;
    private Container hotbarContainer;
    private Container[] slotContainers;
    private Label[] slotLabels;
    private int cachedViewportWidth = -1;

    public HotbarUI(Inventory inventory) {
        this.inventory = Objects.requireNonNull(inventory, "inventory");
    }

    @Override
    protected void initialize(Application app) {
        ensureGuiGlobalsInitialized(app);

        slotContainers = new Container[Inventory.HOTBAR_SIZE];
        slotLabels = new Label[Inventory.HOTBAR_SIZE];

        hotbarContainer = new Container(new SpringGridLayout(Axis.X, Axis.Y, FillMode.Even, FillMode.None));
        for (int i = 0; i < Inventory.HOTBAR_SIZE; i++) {
            Container slotContainer = hotbarContainer.addChild(new Container());
            slotContainer.setBackground(new QuadBackgroundComponent(UNSELECTED_COLOR));
            slotContainer.setPreferredSize(new Vector3f(SLOT_SIZE, SLOT_SIZE, 0f));

            Label label = slotContainer.addChild(new Label(""));
            label.setFontSize(16f);
            label.setColor(ColorRGBA.White);

            slotContainers[i] = slotContainer;
            slotLabels[i] = label;
        }

        SimpleApplication simpleApp = (SimpleApplication) app;
        centerHotbar(simpleApp.getCamera());
        simpleApp.getGuiNode().attachChild(hotbarContainer);

        cachedViewportWidth = app.getCamera().getWidth();

        inventory.addListener(this);
        updateAllSlots();
        updateSelection();

        logger.info("Hotbar UI initialized");
    }

    @Override
    protected void cleanup(Application app) {
        inventory.removeListener(this);
        if (hotbarContainer != null) {
            hotbarContainer.removeFromParent();
        }
        slotContainers = null;
        slotLabels = null;
        logger.info("Hotbar UI cleaned up");
    }

    @Override
    protected void onEnable() {
        setCullHint(Spatial.CullHint.Never);
    }

    @Override
    protected void onDisable() {
        setCullHint(Spatial.CullHint.Always);
    }

    @Override
    public void update(float tpf) {
        Camera camera = getApplication().getCamera();
        int width = camera.getWidth();
        if (width != cachedViewportWidth) {
            centerHotbar(camera);
            cachedViewportWidth = width;
        }
    }

    @Override
    public void onInventoryChanged(Inventory inventory) {
        updateAllSlots();
        updateSelection();
    }

    private void updateAllSlots() {
        for (int i = 0; i < Inventory.HOTBAR_SIZE; i++) {
            ItemStack stack = inventory.getHotbarItem(i);
            slotLabels[i].setText(stack == null ? "" : String.valueOf(stack.count()));
        }
    }

    private void updateSelection() {
        int selected = inventory.getSelectedSlot();
        for (int i = 0; i < Inventory.HOTBAR_SIZE; i++) {
            slotContainers[i].setBackground(new QuadBackgroundComponent(i == selected ? SELECTED_COLOR : UNSELECTED_COLOR));
        }
    }

    private void centerHotbar(Camera camera) {
        float totalWidth = Inventory.HOTBAR_SIZE * SLOT_SIZE + (Inventory.HOTBAR_SIZE - 1) * SLOT_PADDING;
        float x = (camera.getWidth() - totalWidth) / 2f;
        float y = HOTBAR_Y_OFFSET;
        hotbarContainer.setLocalTranslation(x, y, 0f);
    }

    private void ensureGuiGlobalsInitialized(Application app) {
        try {
            GuiGlobals.getInstance();
        } catch (IllegalStateException ignored) {
            GuiGlobals.initialize(app);
        }
    }

    private void setCullHint(Spatial.CullHint hint) {
        if (hotbarContainer != null) {
            hotbarContainer.setCullHint(hint);
        }
    }
}
