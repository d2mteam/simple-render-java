package com.simplerender.camera;

import com.simplerender.math.Vector3f;

/**
 * Immutable snapshot of camera vectors used by the renderer.
 */
public final class CameraSnapshot {
    private final Vector3f position;
    private final Vector3f forward;
    private final Vector3f up;

    public CameraSnapshot(Vector3f position, Vector3f forward, Vector3f up) {
        this.position = position;
        this.forward = forward;
        this.up = up;
    }

    public Vector3f position() {
        return position;
    }

    public Vector3f forward() {
        return forward;
    }

    public Vector3f up() {
        return up;
    }
}
