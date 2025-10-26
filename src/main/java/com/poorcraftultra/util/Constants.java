package com.poorcraftultra.util;

/**
 * Game-wide constants for configuration values.
 */
public final class Constants {

    // Window settings
    public static final int WINDOW_WIDTH = 1280;
    public static final int WINDOW_HEIGHT = 720;
    public static final String WINDOW_TITLE = "PoorCraft Ultra";

    // Camera settings
    public static final float FOV = 70.0f;
    public static final float Z_NEAR = 0.1f;
    public static final float Z_FAR = 1000.0f;

    // Movement settings
    public static final float CAMERA_MOVE_SPEED = 5.0f;
    public static final float CAMERA_SPRINT_MULTIPLIER = 2.0f;

    // Mouse settings
    public static final float MOUSE_SENSITIVITY = 0.1f;

    // Target FPS
    public static final int TARGET_FPS = 60;

    // Rendering settings
    public static final String VERTEX_SHADER_PATH = "/shaders/vertex.glsl";
    public static final String FRAGMENT_SHADER_PATH = "/shaders/fragment.glsl";
    public static final float CUBE_SIZE = 1.0f;

    // Private constructor to prevent instantiation
    private Constants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
