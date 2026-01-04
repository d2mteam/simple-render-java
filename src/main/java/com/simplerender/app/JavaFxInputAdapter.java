package com.simplerender.app;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.application.Platform;
import java.util.EnumSet;
import java.util.Set;

public final class JavaFxInputAdapter {
    private final Object lock = new Object();
    private final Set<KeyCode> pressedKeys = EnumSet.noneOf(KeyCode.class);
    private double lastMouseX;
    private double lastMouseY;
    private double pendingDeltaX;
    private double pendingDeltaY;
    private boolean hasMouseOrigin;
    private boolean cursorInside;

    public void attach(Scene scene, Node inputNode) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> updateKey(event.getCode(), true));
        scene.addEventFilter(KeyEvent.KEY_RELEASED, event -> updateKey(event.getCode(), false));
        inputNode.addEventHandler(MouseEvent.MOUSE_ENTERED, this::handleMouseEnter);
        inputNode.addEventHandler(MouseEvent.MOUSE_EXITED, event -> handleMouseExit());
        inputNode.addEventHandler(MouseEvent.MOUSE_MOVED, this::handleMouseMove);
        inputNode.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleMouseMove);

        // Auto-request focus
        Platform.runLater(inputNode::requestFocus);
    }

    public InputState consumeInput() {
        synchronized (lock) {
            boolean forward = pressedKeys.contains(KeyCode.W);
            boolean backward = pressedKeys.contains(KeyCode.S);
            boolean left = pressedKeys.contains(KeyCode.A);
            boolean right = pressedKeys.contains(KeyCode.D);
            boolean up = pressedKeys.contains(KeyCode.SPACE);
            boolean down = pressedKeys.contains(KeyCode.SHIFT);
            double deltaX = pendingDeltaX;
            double deltaY = pendingDeltaY;
            pendingDeltaX = 0.0;
            pendingDeltaY = 0.0;
            return new InputState(forward, backward, left, right, up, down, deltaX, deltaY);
        }
    }

    private void updateKey(KeyCode code, boolean pressed) {
        synchronized (lock) {
            if (pressed) {
                pressedKeys.add(code);
            } else {
                pressedKeys.remove(code);
            }
        }
    }

    private void handleMouseEnter(MouseEvent event) {
        synchronized (lock) {
            cursorInside = true;
            lastMouseX = event.getX();
            lastMouseY = event.getY();
            hasMouseOrigin = true;
        }
    }

    private void handleMouseExit() {
        synchronized (lock) {
            cursorInside = false;
            hasMouseOrigin = false;
        }
    }

    private void handleMouseMove(MouseEvent event) {
        synchronized (lock) {
            if (!cursorInside) {
                return;
            }
            if (!hasMouseOrigin) {
                lastMouseX = event.getX();
                lastMouseY = event.getY();
                hasMouseOrigin = true;
                return;
            }
            double currentX = event.getX();
            double currentY = event.getY();
            pendingDeltaX += currentX - lastMouseX;
            pendingDeltaY += currentY - lastMouseY;
            lastMouseX = currentX;
            lastMouseY = currentY;
        }
    }
}
