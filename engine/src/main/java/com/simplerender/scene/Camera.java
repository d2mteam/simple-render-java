package com.simplerender.scene;

import com.simplerender.math.Vector3f;

/**
 * First-person camera: a position plus yaw/pitch angles, and the lens settings.
 *
 * <p>Yaw -90° with pitch 0° looks down the -Z axis.
 */
public final class Camera {
    private static final float MAX_PITCH_DEGREES = 89.0f;

    private final Vector3f position;
    private float yawDegrees = -90.0f;
    private float pitchDegrees = 0.0f;
    private float moveSpeed = 2.0f;
    private final float fovDegrees = 60.0f;
    private final float nearPlane = 0.1f;
    private final float farPlane = 100.0f;

    public Camera() {
        this(new Vector3f(0.0f, 0.0f, 5.0f));
    }

    public Camera(Vector3f position) {
        this.position = position.copy();
    }

    /** Turns the camera; pitch is clamped so it never flips over the vertical. */
    public void rotate(float deltaYawDegrees, float deltaPitchDegrees) {
        yawDegrees += deltaYawDegrees;
        pitchDegrees += deltaPitchDegrees;
        pitchDegrees = Math.max(-MAX_PITCH_DEGREES, Math.min(MAX_PITCH_DEGREES, pitchDegrees));
    }

    public void move(float dx, float dy, float dz) {
        position.setX(position.x() + dx);
        position.setY(position.y() + dy);
        position.setZ(position.z() + dz);
    }

    public Vector3f position() {
        return position.copy();
    }

    /** Unit vector the camera looks along, computed from yaw and pitch. */
    public Vector3f forward() {
        float yawRad = (float) Math.toRadians(yawDegrees);
        float pitchRad = (float) Math.toRadians(pitchDegrees);
        float fx = (float) (Math.cos(yawRad) * Math.cos(pitchRad));
        float fy = (float) Math.sin(pitchRad);
        float fz = (float) (Math.sin(yawRad) * Math.cos(pitchRad));
        float length = (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
        return new Vector3f(fx / length, fy / length, fz / length);
    }

    public Vector3f up() {
        return new Vector3f(0.0f, 1.0f, 0.0f);
    }

    /** Movement speed in world units per second. */
    public float moveSpeed() {
        return moveSpeed;
    }

    public void setMoveSpeed(float moveSpeed) {
        this.moveSpeed = moveSpeed;
    }

    public CameraSnapshot snapshot() {
        return new CameraSnapshot(position(), forward(), up(), fovDegrees, nearPlane, farPlane);
    }
}
