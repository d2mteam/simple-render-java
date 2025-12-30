package com.simplerender.camera;

import com.simplerender.app.Time;

public final class CameraController {
    public void update(Camera camera, Time time) {
        // Placeholder for input-driven camera updates.
        camera.position().setZ(camera.position().z() + 0.0f * time.deltaSeconds());
    }
}
