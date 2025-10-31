package com.poorcraft.ultra.engine.api;

/**
 * Represents a component whose input handling can be enabled or disabled.
 * Implementations typically pause or resume camera or player controls when
 * UI overlays such as menus or dialogs are shown.
 */
public interface InputControllable {

    /**
     * Enables or disables this component's input handling.
     *
     * @param enabled {@code true} to allow input processing, {@code false} to suppress it
     */
    void setInputEnabled(boolean enabled);
}
