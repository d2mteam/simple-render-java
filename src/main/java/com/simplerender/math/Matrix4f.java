package com.simplerender.math;

/**
 * Static helpers for common 4x4 matrix operations used by the renderer.
 */
public final class Matrix4f {
    private Matrix4f() {
    }

    public static float[] identity() {
        float[] m = new float[16];
        m[0] = 1.0f;
        m[5] = 1.0f;
        m[10] = 1.0f;
        m[15] = 1.0f;
        return m;
    }

    public static float[] perspective(float fovRadians, float aspect, float near, float far) {
        float[] m = new float[16];
        float f = 1.0f / (float) Math.tan(fovRadians / 2.0f);
        m[0] = f / aspect;
        m[5] = f;
        m[10] = (far + near) / (near - far);
        m[11] = -1.0f;
        m[14] = (2.0f * far * near) / (near - far);
        return m;
    }

    public static float[] lookAt(Vector3f eye, Vector3f center, Vector3f up) {
        Vector3f f = center.subtract(eye).normalize();
        Vector3f s = f.cross(up).normalize();
        Vector3f u = s.cross(f);

        float[] m = identity();
        m[0] = s.x();
        m[4] = s.y();
        m[8] = s.z();

        m[1] = u.x();
        m[5] = u.y();
        m[9] = u.z();

        m[2] = -f.x();
        m[6] = -f.y();
        m[10] = -f.z();

        m[12] = -s.dot(eye);
        m[13] = -u.dot(eye);
        m[14] = f.dot(eye);
        return m;
    }

    public static float[] multiply(float[] left, float[] right) {
        float[] result = new float[16];
        for (int col = 0; col < 4; col++) {
            int colOffset = col * 4;
            for (int row = 0; row < 4; row++) {
                result[colOffset + row] =
                    left[row] * right[colOffset]
                        + left[row + 4] * right[colOffset + 1]
                        + left[row + 8] * right[colOffset + 2]
                        + left[row + 12] * right[colOffset + 3];
            }
        }
        return result;
    }
}
