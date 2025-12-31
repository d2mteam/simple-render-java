package com.simplerender.gl;

import com.simplerender.app.InputState;
import org.lwjgl.glfw.GLFW;

public final class GlfwInputReader {
    private double lastMouseX;
    private double lastMouseY;
    private boolean firstMouse = true;
    private final double[] cursorPosX = new double[1];
    private final double[] cursorPosY = new double[1];

    public InputState readInput(long window) {
        boolean forward = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS;
        boolean backward = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS;
        boolean left = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS;
        boolean right = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS;
        boolean up = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;
        boolean down = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS;

        boolean allowLook = GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_HOVERED) == GLFW.GLFW_TRUE;
        GLFW.glfwGetCursorPos(window, cursorPosX, cursorPosY);
        if (firstMouse) {
            lastMouseX = cursorPosX[0];
            lastMouseY = cursorPosY[0];
            firstMouse = false;
        }
        double deltaX = 0.0;
        double deltaY = 0.0;
        if (allowLook) {
            deltaX = cursorPosX[0] - lastMouseX;
            deltaY = cursorPosY[0] - lastMouseY;
        }
        lastMouseX = cursorPosX[0];
        lastMouseY = cursorPosY[0];

        return new InputState(forward, backward, left, right, up, down, deltaX, deltaY);
    }
}
