package com.simplerender.asset;

import com.simplerender.math.Vector3f;
import java.util.Arrays;

public final class MeshData {
    private final float[] positions;
    private final float[] normals;
    private final int[] indices;
    private final Vector3f boundsCenter;
    private final float boundsRadius;

    public MeshData(float[] positions, float[] normals, int[] indices) {
        this.positions = Arrays.copyOf(positions, positions.length);
        this.normals = Arrays.copyOf(normals, normals.length);
        this.indices = Arrays.copyOf(indices, indices.length);
        Vector3f center = computeBoundsCenter(this.positions);
        this.boundsCenter = center;
        this.boundsRadius = computeBoundsRadius(this.positions, center);
    }

    public float[] positions() {
        return Arrays.copyOf(positions, positions.length);
    }

    public float[] normals() {
        return Arrays.copyOf(normals, normals.length);
    }

    public int[] indices() {
        return Arrays.copyOf(indices, indices.length);
    }

    public int vertexCount() {
        return positions.length / 3;
    }

    public Vector3f boundsCenter() {
        return boundsCenter.copy();
    }

    public float boundsRadius() {
        return boundsRadius;
    }

    private static Vector3f computeBoundsCenter(float[] positions) {
        if (positions.length < 3) {
            return new Vector3f(0.0f, 0.0f, 0.0f);
        }
        float minX = positions[0];
        float minY = positions[1];
        float minZ = positions[2];
        float maxX = positions[0];
        float maxY = positions[1];
        float maxZ = positions[2];
        for (int i = 3; i < positions.length; i += 3) {
            float x = positions[i];
            float y = positions[i + 1];
            float z = positions[i + 2];
            if (x < minX) {
                minX = x;
            }
            if (y < minY) {
                minY = y;
            }
            if (z < minZ) {
                minZ = z;
            }
            if (x > maxX) {
                maxX = x;
            }
            if (y > maxY) {
                maxY = y;
            }
            if (z > maxZ) {
                maxZ = z;
            }
        }
        return new Vector3f(
            (minX + maxX) * 0.5f,
            (minY + maxY) * 0.5f,
            (minZ + maxZ) * 0.5f
        );
    }

    private static float computeBoundsRadius(float[] positions, Vector3f center) {
        if (positions.length < 3) {
            return 0.0f;
        }
        float maxDistanceSq = 0.0f;
        for (int i = 0; i < positions.length; i += 3) {
            float dx = positions[i] - center.x();
            float dy = positions[i + 1] - center.y();
            float dz = positions[i + 2] - center.z();
            float distanceSq = dx * dx + dy * dy + dz * dz;
            if (distanceSq > maxDistanceSq) {
                maxDistanceSq = distanceSq;
            }
        }
        return (float) Math.sqrt(maxDistanceSq);
    }
}
