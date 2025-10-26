package com.poorcraftultra.input;

import com.poorcraftultra.rendering.Camera;
import com.poorcraftultra.util.Constants;
import org.lwjgl.glfw.GLFW;

/**
 * Mouse input handler for first-person camera rotation.
 */
public class MouseInput {

    private long windowHandle;
    private double lastX;
    private double lastY;
    private double xoffset;
    private double yoffset;
    private boolean firstMouse;

    public MouseInput(long windowHandle) {
        this.windowHandle = windowHandle;
        this.firstMouse = true;

        // Disable cursor and enable raw mouse input
        GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);

        if (GLFW.glfwRawMouseMotionSupported()) {
            GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_RAW_MOUSE_MOTION, GLFW.GLFW_TRUE);
        }

        // Set cursor position callback
        GLFW.glfwSetCursorPosCallback(windowHandle, (window, xpos, ypos) -> {
            if (firstMouse) {
                lastX = xpos;
                lastY = ypos;
                firstMouse = false;
            }

            xoffset = xpos - lastX;
            yoffset = lastY - ypos; // Reversed since y-coordinates go from bottom to top

            lastX = xpos;
            lastY = ypos;
        });
    }

    public void processInput(Camera camera, float deltaTime) {
        if (firstMouse) {
            return; // Skip first frame
        }

        float sensitivity = Constants.MOUSE_SENSITIVITY;
        xoffset *= sensitivity;
        yoffset *= sensitivity;

        camera.rotate((float) xoffset, (float) yoffset);

        // Reset offsets after processing
        xoffset = 0;
        yoffset = 0;
    }

    public double getXOffset() {
        return xoffset;
    }

    public double getYOffset() {
        return yoffset;
    }

    public double getLastX() {
        return lastX;
    }

    public double getLastY() {
        return lastY;
    }

    // Method for testing
    public void setMouseOffsets(double xoffset, double yoffset) {
        this.xoffset = xoffset;
        this.yoffset = yoffset;
        this.firstMouse = false;
    }
}
