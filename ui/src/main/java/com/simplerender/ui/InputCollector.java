package com.simplerender.ui;

import com.simplerender.scene.InputState;
import java.util.EnumSet;
import java.util.Set;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

/**
 * Turns JavaFX key and mouse events on the render view into one {@link InputState} per frame.
 *
 * <p>Events arrive on the JavaFX thread; {@link #poll()} is called by the engine's render
 * thread, so the shared state is guarded by {@code this}.
 *
 * <p>Keys only count while the view has focus (so typing in a text field does not move the
 * camera), and the mouse only turns the camera while a button is held down.
 */
final class InputCollector {
    private final Set<KeyCode> pressedKeys = EnumSet.noneOf(KeyCode.class);
    private double lastMouseX;
    private double lastMouseY;
    private double pendingDeltaX;
    private double pendingDeltaY;

    void attachTo(Node view) {
        view.setFocusTraversable(true);
        view.addEventHandler(KeyEvent.KEY_PRESSED, event -> setKey(event.getCode(), true));
        view.addEventHandler(KeyEvent.KEY_RELEASED, event -> setKey(event.getCode(), false));
        view.focusedProperty().addListener((obs, wasFocused, focused) -> {
            if (!focused) {
                releaseAllKeys(); // otherwise a key released elsewhere would stay "held"
            }
        });
        view.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            view.requestFocus();
            startDrag(event.getX(), event.getY());
        });
        view.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> drag(event.getX(), event.getY()));
    }

    /** Returns the input since the last call and resets the mouse movement. */
    synchronized InputState poll() {
        InputState state = new InputState(
                pressedKeys.contains(KeyCode.W),
                pressedKeys.contains(KeyCode.S),
                pressedKeys.contains(KeyCode.A),
                pressedKeys.contains(KeyCode.D),
                pressedKeys.contains(KeyCode.SPACE),
                pressedKeys.contains(KeyCode.SHIFT),
                pendingDeltaX,
                pendingDeltaY);
        pendingDeltaX = 0.0;
        pendingDeltaY = 0.0;
        return state;
    }

    private synchronized void setKey(KeyCode code, boolean pressed) {
        if (pressed) {
            pressedKeys.add(code);
        } else {
            pressedKeys.remove(code);
        }
    }

    private synchronized void releaseAllKeys() {
        pressedKeys.clear();
    }

    private synchronized void startDrag(double x, double y) {
        lastMouseX = x;
        lastMouseY = y;
    }

    private synchronized void drag(double x, double y) {
        pendingDeltaX += x - lastMouseX;
        pendingDeltaY += y - lastMouseY;
        lastMouseX = x;
        lastMouseY = y;
    }
}
