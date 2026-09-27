package com.simplerender.render;

import com.simplerender.asset.MeshData;
import com.simplerender.math.Matrix4f;
import com.simplerender.math.Vector3f;

/**
 * Skips objects that are completely outside the camera's view.
 *
 * <p>Extracts the six frustum planes from {@code projection * view} and tests each mesh's
 * bounding sphere, transformed to world space, against them.
 */
final class FrustumCuller {
    private final float[] planes = new float[24]; // 6 planes x (a, b, c, d), normalised

    void update(float[] projectionMatrix, float[] viewMatrix) {
        float[] clip = Matrix4f.multiply(projectionMatrix, viewMatrix);
        setPlane(0, clip[3] + clip[0], clip[7] + clip[4], clip[11] + clip[8], clip[15] + clip[12]);
        setPlane(1, clip[3] - clip[0], clip[7] - clip[4], clip[11] - clip[8], clip[15] - clip[12]);
        setPlane(2, clip[3] + clip[1], clip[7] + clip[5], clip[11] + clip[9], clip[15] + clip[13]);
        setPlane(3, clip[3] - clip[1], clip[7] - clip[5], clip[11] - clip[9], clip[15] - clip[13]);
        setPlane(4, clip[3] + clip[2], clip[7] + clip[6], clip[11] + clip[10], clip[15] + clip[14]);
        setPlane(5, clip[3] - clip[2], clip[7] - clip[6], clip[11] - clip[10], clip[15] - clip[14]);
    }

    /** True if the mesh's bounding sphere, placed by {@code modelMatrix}, touches the frustum. */
    boolean isVisible(MeshData mesh, float[] modelMatrix) {
        Vector3f center = mesh.boundsCenter();
        float radius = mesh.boundsRadius();
        if (radius <= 0.0f) {
            return true;
        }
        float[] m = modelMatrix;
        float worldX = m[0] * center.x() + m[4] * center.y() + m[8] * center.z() + m[12];
        float worldY = m[1] * center.x() + m[5] * center.y() + m[9] * center.z() + m[13];
        float worldZ = m[2] * center.x() + m[6] * center.y() + m[10] * center.z() + m[14];
        // Non-uniform scale stretches the sphere; use the largest axis to stay conservative.
        float scaleX = (float) Math.sqrt(m[0] * m[0] + m[1] * m[1] + m[2] * m[2]);
        float scaleY = (float) Math.sqrt(m[4] * m[4] + m[5] * m[5] + m[6] * m[6]);
        float scaleZ = (float) Math.sqrt(m[8] * m[8] + m[9] * m[9] + m[10] * m[10]);
        float maxScale = Math.max(scaleX, Math.max(scaleY, scaleZ));
        return isSphereVisible(worldX, worldY, worldZ, radius * maxScale);
    }

    boolean isSphereVisible(float x, float y, float z, float radius) {
        for (int i = 0; i < 6; i++) {
            int offset = i * 4;
            float distance = planes[offset] * x + planes[offset + 1] * y + planes[offset + 2] * z + planes[offset + 3];
            if (distance < -radius) {
                return false;
            }
        }
        return true;
    }

    private void setPlane(int index, float a, float b, float c, float d) {
        int offset = index * 4;
        float length = (float) Math.sqrt(a * a + b * b + c * c);
        float invLength = length == 0.0f ? 0.0f : 1.0f / length;
        planes[offset] = a * invLength;
        planes[offset + 1] = b * invLength;
        planes[offset + 2] = c * invLength;
        planes[offset + 3] = d * invLength;
    }
}
