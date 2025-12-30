package com.simplerender.camera;

import com.simplerender.app.Time;

public final class CameraController {
    public void update(Camera camera, Time time) {
        float t = (float) (System.currentTimeMillis() * 0.001);
        float radius = 6.0f;
        float height = 3.0f;
        camera.position().setX((float) Math.cos(t) * radius);
        camera.position().setZ((float) Math.sin(t) * radius);
        camera.position().setY(height + (float) Math.sin(t * 0.5f));
        float dx = -camera.position().x();
        float dy = -camera.position().y();
        float dz = -camera.position().z();
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 0.0f) {
            camera.forward().setX(dx / len);
            camera.forward().setY(dy / len);
            camera.forward().setZ(dz / len);
        }
    }
}
