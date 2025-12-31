package com.simplerender.camera;

import com.simplerender.app.InputState;
import com.simplerender.app.Time;

public final class CameraController {
    private float yaw = -90.0f;
    private float pitch = 0.0f;

    public void update(Camera camera, Time time, InputState inputState) {
        float sensitivity = 0.15f;
        yaw += (float) inputState.mouseDeltaX() * sensitivity;
        pitch -= (float) inputState.mouseDeltaY() * sensitivity;
        if (pitch > 89.0f) {
            pitch = 89.0f;
        }
        if (pitch < -89.0f) {
            pitch = -89.0f;
        }

        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);
        float fx = (float) (Math.cos(yawRad) * Math.cos(pitchRad));
        float fy = (float) Math.sin(pitchRad);
        float fz = (float) (Math.sin(yawRad) * Math.cos(pitchRad));
        float fLen = (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
        if (fLen > 0.0f) {
            camera.forward().setX(fx / fLen);
            camera.forward().setY(fy / fLen);
            camera.forward().setZ(fz / fLen);
        }

        float speed = 4.0f * time.deltaSeconds();
        float moveX = 0.0f;
        float moveY = 0.0f;
        float moveZ = 0.0f;

        if (inputState.forward()) {
            moveX += camera.forward().x();
            moveY += camera.forward().y();
            moveZ += camera.forward().z();
        }
        if (inputState.backward()) {
            moveX -= camera.forward().x();
            moveY -= camera.forward().y();
            moveZ -= camera.forward().z();
        }
        float rightX = -camera.forward().z();
        float rightZ = camera.forward().x();
        float rightLen = (float) Math.sqrt(rightX * rightX + rightZ * rightZ);
        if (rightLen > 0.0f) {
            rightX /= rightLen;
            rightZ /= rightLen;
        }
        if (inputState.right()) {
            moveX += rightX;
            moveZ += rightZ;
        }
        if (inputState.left()) {
            moveX -= rightX;
            moveZ -= rightZ;
        }
        if (inputState.up()) {
            moveY += 1.0f;
        }
        if (inputState.down()) {
            moveY -= 1.0f;
        }

        float moveLen = (float) Math.sqrt(moveX * moveX + moveY * moveY + moveZ * moveZ);
        if (moveLen > 0.0f) {
            moveX /= moveLen;
            moveY /= moveLen;
            moveZ /= moveLen;
        }

        camera.position().setX(camera.position().x() + moveX * speed);
        camera.position().setY(camera.position().y() + moveY * speed);
        camera.position().setZ(camera.position().z() + moveZ * speed);
    }
}
