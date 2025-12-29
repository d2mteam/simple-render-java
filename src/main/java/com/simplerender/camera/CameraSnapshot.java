package com.simplerender.camera;

import com.simplerender.math.Vector3f;

public final class CameraSnapshot {
    private final Vector3f position;
    private final Vector3f forward;

    public CameraSnapshot(Vector3f position, Vector3f forward) {
        this.position = position;
        this.forward = forward;
    }

    public Vector3f position() {
        return position;
    }

    public Vector3f forward() {
        return forward;
    }
}
