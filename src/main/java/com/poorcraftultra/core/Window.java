package com.poorcraftultra.core;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;

public final class Window {
    private final String title;
    private final int width;
    private final int height;
    private final boolean resizable;
    private final boolean vSync;

    private long handle;

    public Window(String title, int width, int height) {
        this.title = title;
        this.width = width;
        this.height = height;
        this.resizable = false;
        this.vSync = true;
        this.handle = MemoryUtil.NULL;
    }

    public void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW \n(╯°□°)╯︵ ┻━┻");
        }

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, resizable ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        handle = GLFW.glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL);
        if (handle == MemoryUtil.NULL) {
            throw new IllegalStateException("Failed to create GLFW window \n(ಥ﹏ಥ)");
        }

        setupKeyCallback();
        centerWindow();

        GLFW.glfwMakeContextCurrent(handle);
        if (vSync) {
            GLFW.glfwSwapInterval(1);
        }

        GL.createCapabilities();
        GLFW.glfwShowWindow(handle);

        System.out.printf("Window created: %dx%d \n\\(^_^)/\n", width, height);
    }

    public boolean shouldClose() {
        return GLFW.glfwWindowShouldClose(handle);
    }

    public void swapBuffers() {
        GLFW.glfwSwapBuffers(handle);
    }

    public void pollEvents() {
        GLFW.glfwPollEvents();
    }

    public void cleanup() {
        Callbacks.glfwFreeCallbacks(handle);
        GLFW.glfwDestroyWindow(handle);
        GLFW.glfwTerminate();
        GLFWErrorCallback previousCallback = GLFW.glfwSetErrorCallback(null);
        if (previousCallback != null) {
            previousCallback.free();
        }
        System.out.println("Window destroyed.\n(ノಠ益ಠ)ノ彡┻━┻");
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public long getHandle() {
        return handle;
    }

    private void setupKeyCallback() {
        GLFW.glfwSetKeyCallback(handle, (window, key, scancode, action, mods) -> {
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE) {
                GLFW.glfwSetWindowShouldClose(window, true);
                System.out.println("ESC pressed, closing window.\n(ง'̀-'́)ง");
            }
        });
    }

    private void centerWindow() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            GLFW.glfwGetWindowSize(handle, pWidth, pHeight);
            GLFWVidMode videoMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
            if (videoMode != null) {
                int x = (videoMode.width() - pWidth.get(0)) / 2;
                int y = (videoMode.height() - pHeight.get(0)) / 2;
                GLFW.glfwSetWindowPos(handle, x, y);
            }
        }
    }
}
