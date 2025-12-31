package com.simplerender.render.culling;

import com.simplerender.math.Matrix4f;
import com.simplerender.math.Vector3f;

public final class FrustumCuller {
    private final float[] planes = new float[24];

    public void update(float[] projectionMatrix, float[] viewMatrix) {
        float[] clip = Matrix4f.multiply(projectionMatrix, viewMatrix);
        extractPlanes(clip);
    }

    public boolean isVisible(Vector3f center, float radius) {
        for (int i = 0; i < 6; i++) {
            int offset = i * 4;
            float distance =
                planes[offset] * center.x()
                    + planes[offset + 1] * center.y()
                    + planes[offset + 2] * center.z()
                    + planes[offset + 3];
            if (distance < -radius) {
                return false;
            }
        }
        return true;
    }

    private void extractPlanes(float[] clip) {
        setPlane(0, clip[3] + clip[0], clip[7] + clip[4], clip[11] + clip[8], clip[15] + clip[12]);
        setPlane(1, clip[3] - clip[0], clip[7] - clip[4], clip[11] - clip[8], clip[15] - clip[12]);
        setPlane(2, clip[3] + clip[1], clip[7] + clip[5], clip[11] + clip[9], clip[15] + clip[13]);
        setPlane(3, clip[3] - clip[1], clip[7] - clip[5], clip[11] - clip[9], clip[15] - clip[13]);
        setPlane(4, clip[3] + clip[2], clip[7] + clip[6], clip[11] + clip[10], clip[15] + clip[14]);
        setPlane(5, clip[3] - clip[2], clip[7] - clip[6], clip[11] - clip[10], clip[15] - clip[14]);
    }

    private void setPlane(int index, float a, float b, float c, float d) {
        float length = (float) Math.sqrt(a * a + b * b + c * c);
        int offset = index * 4;
        if (length == 0.0f) {
            planes[offset] = 0.0f;
            planes[offset + 1] = 0.0f;
            planes[offset + 2] = 0.0f;
            planes[offset + 3] = 0.0f;
            return;
        }
        float invLength = 1.0f / length;
        planes[offset] = a * invLength;
        planes[offset + 1] = b * invLength;
        planes[offset + 2] = c * invLength;
        planes[offset + 3] = d * invLength;
    }
}
