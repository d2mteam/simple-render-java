package com.simplerender.camera;

import com.simplerender.math.Vector3f;

public final class Camera {
    private final Vector3f position;
    private final Vector3f forward;
    private final Vector3f up;
    private float moveSpeed = 2.0f;

    public Camera() {
        this.position = new Vector3f(0.0f, 0.0f, 5.0f);
        this.forward = new Vector3f(0.0f, 0.0f, -1.0f);
        this.up = new Vector3f(0.0f, 1.0f, 0.0f);
    }

    public float moveSpeed() {
        return moveSpeed;
    }

    public void setMoveSpeed(float moveSpeed) {
        this.moveSpeed = moveSpeed;
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

    public CameraSnapshot snapshot() {
        return new CameraSnapshot(position.copy(), forward.copy(), up.copy());
    }
}
