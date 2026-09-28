package com.simplerender.scene;

/**
 * The player input for one frame: which movement keys are held and how far the mouse moved.
 *
 * <p>The front-end (JavaFX, a test, ...) produces these; {@link CameraController} consumes them.
 */
public record InputState(
        boolean forward,
        boolean backward,
        boolean left,
        boolean right,
        boolean up,
        boolean down,
        double mouseDeltaX,
        double mouseDeltaY) {

    /** No keys held, no mouse movement. */
    public static final InputState IDLE = new InputState(false, false, false, false, false, false, 0.0, 0.0);
}
