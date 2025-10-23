package com.poorcraftultra.input;

/**
 * Represents all possible player actions in the game.
 * 
 * <p>This enum abstracts input actions from physical keys, enabling rebinding.
 * Each action has a user-friendly display name for UI purposes.
 */
public enum InputAction {
    MOVE_FORWARD("Move Forward"),
    MOVE_BACKWARD("Move Backward"),
    MOVE_LEFT("Move Left"),
    MOVE_RIGHT("Move Right"),
    JUMP("Jump"),
    SPRINT("Sprint"),
    CROUCH("Crouch"),
    TOGGLE_CURSOR("Toggle Cursor");
    
    private final String displayName;
    
    /**
     * Creates an input action with a display name.
     * 
     * @param displayName User-friendly name for this action
     */
    InputAction(String displayName) {
        this.displayName = displayName;
    }
    
    /**
     * Gets the user-friendly display name.
     * 
     * @return Display name for UI purposes
     */
    public String getDisplayName() {
        return displayName;
    }
}
