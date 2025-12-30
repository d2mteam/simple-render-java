package com.simplerender.camera;

import com.simplerender.app.Time;

public final class CameraController {
    public void update(Camera camera, Time time) {
        float drift = 0.1f * time.deltaSeconds();
        camera.position().setZ(camera.position().z() + drift);
    }
}
