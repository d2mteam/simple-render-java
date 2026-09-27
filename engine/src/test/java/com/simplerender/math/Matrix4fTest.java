package com.simplerender.math;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class Matrix4fTest {
    private static final float EPSILON = 1e-5f;

    @Test
    void multiplyingByIdentityChangesNothing() {
        float[] m = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
        assertArrayEquals(m, Matrix4f.multiply(Matrix4f.identity(), m), EPSILON);
        assertArrayEquals(m, Matrix4f.multiply(m, Matrix4f.identity()), EPSILON);
    }

    @Test
    void lookAtMovesEyeToOriginLookingDownMinusZ() {
        Vector3f eye = new Vector3f(1, 2, 3);
        float[] view = Matrix4f.lookAt(eye, new Vector3f(1, 2, 0), new Vector3f(0, 1, 0));
        assertArrayEquals(new float[] { 0, 0, 0 }, transformPoint(view, 1, 2, 3), EPSILON);
        assertArrayEquals(new float[] { 0, 0, -3 }, transformPoint(view, 1, 2, 0), EPSILON);
    }

    private static float[] transformPoint(float[] m, float x, float y, float z) {
        return new float[] {
                m[0] * x + m[4] * y + m[8] * z + m[12],
                m[1] * x + m[5] * y + m[9] * z + m[13],
                m[2] * x + m[6] * y + m[10] * z + m[14],
        };
    }
}
