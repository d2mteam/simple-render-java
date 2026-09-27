package com.simplerender.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.simplerender.asset.MeshData;
import com.simplerender.math.Matrix4f;
import com.simplerender.math.Vector3f;
import org.junit.jupiter.api.Test;

class FrustumCullerTest {
    private static final MeshData UNIT_TRIANGLE = new MeshData(
            new float[] { -1, -1, 0, 1, -1, 0, 0, 1, 0 },
            new float[] { 0, 0, 1, 0, 0, 1, 0, 0, 1 },
            null,
            new int[] { 0, 1, 2 });

    private final FrustumCuller culler = new FrustumCuller();

    FrustumCullerTest() {
        // Camera at the origin looking down -Z, 60 degree FOV, near 0.1, far 100.
        float[] view = Matrix4f.lookAt(new Vector3f(0, 0, 0), new Vector3f(0, 0, -1), new Vector3f(0, 1, 0));
        float[] projection = Matrix4f.perspective((float) Math.toRadians(60), 1.0f, 0.1f, 100.0f);
        culler.update(projection, view);
    }

    @Test
    void objectInFrontIsVisible() {
        assertTrue(culler.isVisible(UNIT_TRIANGLE, translation(0, 0, -5)));
    }

    @Test
    void objectsBehindOrBeyondFarPlaneAreCulled() {
        assertFalse(culler.isVisible(UNIT_TRIANGLE, translation(0, 0, 5)));
        assertFalse(culler.isVisible(UNIT_TRIANGLE, translation(0, 0, -200)));
        assertFalse(culler.isVisible(UNIT_TRIANGLE, translation(50, 0, -5)));
    }

    @Test
    void scaleGrowsTheBoundingSphere() {
        float[] farRightButHuge = translation(8, 0, -5);
        farRightButHuge[0] = farRightButHuge[5] = farRightButHuge[10] = 10.0f;
        assertTrue(culler.isVisible(UNIT_TRIANGLE, farRightButHuge));
    }

    private static float[] translation(float x, float y, float z) {
        float[] m = Matrix4f.identity();
        m[12] = x;
        m[13] = y;
        m[14] = z;
        return m;
    }
}
