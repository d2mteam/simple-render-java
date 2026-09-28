package com.simplerender.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.simplerender.math.Vector3f;
import org.junit.jupiter.api.Test;

class CameraControllerTest {
    private static final float EPSILON = 1e-4f;
    private final CameraController controller = new CameraController();

    @Test
    void startsLookingDownMinusZ() {
        Vector3f forward = new Camera().forward();
        assertEquals(0.0f, forward.x(), EPSILON);
        assertEquals(0.0f, forward.y(), EPSILON);
        assertEquals(-1.0f, forward.z(), EPSILON);
    }

    @Test
    void forwardKeyMovesAlongViewDirectionAtMoveSpeed() {
        Camera camera = new Camera(new Vector3f(0, 0, 0));
        camera.setMoveSpeed(2.0f);
        controller.update(camera, 0.5f, new InputState(true, false, false, false, false, false, 0, 0));
        assertEquals(-1.0f, camera.position().z(), EPSILON);
        assertEquals(0.0f, camera.position().x(), EPSILON);
    }

    @Test
    void mouseTurnsCameraAndPitchIsClamped() {
        Camera camera = new Camera();
        // 360 px at 0.25 deg/px = 90 deg to the right: now looking down +X.
        controller.update(camera, 0.0f, new InputState(false, false, false, false, false, false, 360, 0));
        assertEquals(1.0f, camera.forward().x(), EPSILON);

        // A huge upward movement stops just short of straight up instead of flipping over.
        controller.update(camera, 0.0f, new InputState(false, false, false, false, false, false, 0, -100_000));
        assertEquals((float) Math.sin(Math.toRadians(89.0)), camera.forward().y(), EPSILON);
    }
}
