package com.poorcraft.ultra.engine.api;

/**
 * Central definition of input mapping names used by the engine and gameplay systems.
 * Keeping these constants in a shared location avoids string duplication and typos
 * across modules that register or handle input actions.
 */
public final class InputMappings {

    private InputMappings() {
        throw new AssertionError("No instances");
    }

    public static final String MOVE_FORWARD = "MoveForward";
    public static final String MOVE_BACKWARD = "MoveBackward";
    public static final String MOVE_LEFT = "MoveLeft";
    public static final String MOVE_RIGHT = "MoveRight";
    public static final String MOVE_UP = "MoveUp";
    public static final String MOVE_DOWN = "MoveDown";

    public static final String MOUSE_X_POS = "MouseXPos";
    public static final String MOUSE_X_NEG = "MouseXNeg";
    public static final String MOUSE_Y_POS = "MouseYPos";
    public static final String MOUSE_Y_NEG = "MouseYNeg";

    public static final String BREAK_BLOCK = "BreakBlock";
    public static final String PLACE_BLOCK = "PlaceBlock";

    public static final String TOGGLE_MENU = "ToggleMenu";
    public static final String TOGGLE_WIREFRAME = "ToggleWireframe";

    public static final String SELECT_SLOT_1 = "SelectSlot1";
    public static final String SELECT_SLOT_2 = "SelectSlot2";
    public static final String SELECT_SLOT_3 = "SelectSlot3";
    public static final String SELECT_SLOT_4 = "SelectSlot4";
    public static final String SELECT_SLOT_5 = "SelectSlot5";
    public static final String SELECT_SLOT_6 = "SelectSlot6";
    public static final String SELECT_SLOT_7 = "SelectSlot7";
    public static final String SELECT_SLOT_8 = "SelectSlot8";
    public static final String SELECT_SLOT_9 = "SelectSlot9";
}
