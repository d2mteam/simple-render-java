package com.simplerender.scene;

import com.simplerender.math.Vector3f;

/**
 * Fly-camera controls: the mouse turns the camera, WASD moves along the view direction,
 * up/down move along the world Y axis.
 */
public final class CameraController {
    private static final float MOUSE_SENSITIVITY = 0.25f; // degrees per pixel

    public void update(Camera camera, float deltaSeconds, InputState input) {
        camera.rotate(
                (float) input.mouseDeltaX() * MOUSE_SENSITIVITY,
                -(float) input.mouseDeltaY() * MOUSE_SENSITIVITY);

        Vector3f forward = camera.forward();
        float moveX = 0.0f;
        float moveY = 0.0f;
        float moveZ = 0.0f;
        if (input.forward()) {
            moveX += forward.x();
            moveY += forward.y();
            moveZ += forward.z();
        }
        if (input.backward()) {
            moveX -= forward.x();
            moveY -= forward.y();
            moveZ -= forward.z();
        }

        // "Right" stays horizontal so strafing never changes height.
        float rightX = -forward.z();
        float rightZ = forward.x();
        float rightLength = (float) Math.sqrt(rightX * rightX + rightZ * rightZ);
        if (rightLength > 0.0f) {
            rightX /= rightLength;
            rightZ /= rightLength;
        }
        if (input.right()) {
            moveX += rightX;
            moveZ += rightZ;
        }
        if (input.left()) {
            moveX -= rightX;
            moveZ -= rightZ;
        }
        if (input.up()) {
            moveY += 1.0f;
        }
        if (input.down()) {
            moveY -= 1.0f;
        }

        // Normalise so moving diagonally is not faster.
        float moveLength = (float) Math.sqrt(moveX * moveX + moveY * moveY + moveZ * moveZ);
        if (moveLength > 0.0f) {
            moveX /= moveLength;
            moveY /= moveLength;
            moveZ /= moveLength;
        }
        float distance = camera.moveSpeed() * deltaSeconds;
        camera.move(moveX * distance, moveY * distance, moveZ * distance);
    }
}
