package com.simplerender.render.gl;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An OpenGL context with no visible window.
 *
 * <p>The engine renders into its own framebuffers and hands the pixels to the front-end,
 * so GLFW is only used to get a context: it creates a hidden 1x1 window. The context is
 * current on the thread that created it, and every GL call must happen on that thread.
 */
public final class GlContext implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(GlContext.class);

    private final long window;
    private final GLFWErrorCallback errorCallback;

    private GlContext(long window, GLFWErrorCallback errorCallback) {
        this.window = window;
        this.errorCallback = errorCallback;
    }

    public static GlContext createOffscreen() {
        GLFWErrorCallback errorCallback = GLFWErrorCallback.create(
                (error, description) -> logger.error("GLFW error {}: {}", error,
                        GLFWErrorCallback.getDescription(description)));
        GLFW.glfwSetErrorCallback(errorCallback);
        if (!GLFW.glfwInit()) {
            errorCallback.free();
            throw new IllegalStateException("Could not initialise GLFW");
        }
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);
        long window = GLFW.glfwCreateWindow(1, 1, "Simple Render (offscreen)", 0, 0);
        if (window == 0) {
            GLFW.glfwTerminate();
            errorCallback.free();
            throw new IllegalStateException("Could not create an OpenGL context");
        }
        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();
        logger.info("OpenGL {} on {}", GL11.glGetString(GL11.GL_VERSION), GL11.glGetString(GL11.GL_RENDERER));
        return new GlContext(window, errorCallback);
    }

    @Override
    public void close() {
        GL.setCapabilities(null);
        GLFW.glfwMakeContextCurrent(0);
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
        GLFW.glfwSetErrorCallback(null);
        errorCallback.free();
    }
}
