package com.simplerender.scene;

import com.simplerender.math.Vector3f;

/**
 * Read-only copy of the camera for one frame.
 *
 * @param fovDegrees vertical field of view
 */
public record CameraSnapshot(
        Vector3f position,
        Vector3f forward,
        Vector3f up,
        float fovDegrees,
        float nearPlane,
        float farPlane) {
}
