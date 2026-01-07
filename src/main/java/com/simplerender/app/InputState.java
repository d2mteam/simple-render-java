package com.simplerender.app;

/**
 * Immutable input snapshot for a single frame.
 */
public final class InputState {
    private static final InputState IDLE = new InputState(
        false,
        false,
        false,
        false,
        false,
        false,
        0.0,
        0.0
    );

    private final boolean forward;
    private final boolean backward;
    private final boolean left;
    private final boolean right;
    private final boolean up;
    private final boolean down;
    private final double mouseDeltaX;
    private final double mouseDeltaY;

    public InputState(
        boolean forward,
        boolean backward,
        boolean left,
        boolean right,
        boolean up,
        boolean down,
        double mouseDeltaX,
        double mouseDeltaY
    ) {
        this.forward = forward;
        this.backward = backward;
        this.left = left;
        this.right = right;
        this.up = up;
        this.down = down;
        this.mouseDeltaX = mouseDeltaX;
        this.mouseDeltaY = mouseDeltaY;
    }

    public static InputState idle() {
        return IDLE;
    }

    public boolean forward() {
        return forward;
    }

    public boolean backward() {
        return backward;
    }

    public boolean left() {
        return left;
    }

    public boolean right() {
        return right;
    }

    public boolean up() {
        return up;
    }

    public boolean down() {
        return down;
    }

    public double mouseDeltaX() {
        return mouseDeltaX;
    }

    public double mouseDeltaY() {
        return mouseDeltaY;
    }
}
