package com.simplerender.asset;

import com.simplerender.math.Vector3f;
import java.util.Arrays;

/**
 * Immutable triangle mesh on the CPU: vertex attributes, indices and a bounding sphere.
 *
 * <p>Every vertex has a position, normal, two UV sets, tangent and bitangent. Missing UVs
 * are filled with a constant and missing tangents are generated, so the renderer can
 * always upload the same vertex layout. The bounding sphere is used for frustum culling.
 */
public final class MeshData {
    private final float[] positions;
    private final float[] normals;
    private final float[] texCoords0;
    private final float[] texCoords1;
    private final float[] tangents;
    private final float[] bitangents;
    private final int[] indices;
    private final Vector3f boundsCenter;
    private final float boundsRadius;

    /** Mesh with one UV set; tangents are generated. */
    public MeshData(float[] positions, float[] normals, float[] texCoords, int[] indices) {
        this(positions, normals, texCoords, null, indices);
    }

    /** Mesh with up to two UV sets; tangents are generated from UV set 0. */
    public MeshData(float[] positions, float[] normals, float[] texCoords0, float[] texCoords1, int[] indices) {
        this(positions, normals, null, null, texCoords0, texCoords1, indices);
    }

    /**
     * Mesh with explicit tangents. Pass {@code null} tangents/bitangents to generate them.
     */
    public MeshData(
            float[] positions,
            float[] normals,
            float[] tangents,
            float[] bitangents,
            float[] texCoords0,
            float[] texCoords1,
            int[] indices) {
        this.positions = Arrays.copyOf(positions, positions.length);
        this.normals = Arrays.copyOf(normals, normals.length);
        int vertexCount = this.positions.length / 3;
        this.texCoords0 = uvSetOrFallback(texCoords0, vertexCount, null);
        this.texCoords1 = uvSetOrFallback(texCoords1, vertexCount, this.texCoords0);
        this.indices = Arrays.copyOf(indices, indices.length);
        if (tangents != null && bitangents != null) {
            this.tangents = Arrays.copyOf(tangents, tangents.length);
            this.bitangents = Arrays.copyOf(bitangents, bitangents.length);
        } else {
            TangentGenerator.Tangents generated =
                    TangentGenerator.generate(this.positions, this.normals, this.texCoords0, this.indices);
            this.tangents = generated.tangents();
            this.bitangents = generated.bitangents();
        }
        this.boundsCenter = computeBoundsCenter(this.positions);
        this.boundsRadius = computeBoundsRadius(this.positions, boundsCenter);
    }

    public float[] positions() {
        return Arrays.copyOf(positions, positions.length);
    }

    public float[] normals() {
        return Arrays.copyOf(normals, normals.length);
    }

    public float[] texCoords0() {
        return Arrays.copyOf(texCoords0, texCoords0.length);
    }

    public float[] texCoords1() {
        return Arrays.copyOf(texCoords1, texCoords1.length);
    }

    public float[] tangents() {
        return Arrays.copyOf(tangents, tangents.length);
    }

    public float[] bitangents() {
        return Arrays.copyOf(bitangents, bitangents.length);
    }

    public int[] indices() {
        return Arrays.copyOf(indices, indices.length);
    }

    public int vertexCount() {
        return positions.length / 3;
    }

    /** Centre of the bounding sphere, in the mesh's local space. */
    public Vector3f boundsCenter() {
        return boundsCenter.copy();
    }

    public float boundsRadius() {
        return boundsRadius;
    }

    /**
     * Returns a copy of {@code texCoords} if it has one UV per vertex; otherwise a copy of
     * {@code fallbackSource} (if valid) or a constant (0.5, 0.5) for every vertex.
     */
    private static float[] uvSetOrFallback(float[] texCoords, int vertexCount, float[] fallbackSource) {
        int expectedLength = vertexCount * 2;
        if (texCoords != null && texCoords.length == expectedLength) {
            return Arrays.copyOf(texCoords, texCoords.length);
        }
        if (fallbackSource != null && fallbackSource.length == expectedLength) {
            return Arrays.copyOf(fallbackSource, expectedLength);
        }
        float[] fallback = new float[expectedLength];
        Arrays.fill(fallback, 0.5f);
        return fallback;
    }

    /** Centre of the axis-aligned bounding box. */
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
            minX = Math.min(minX, positions[i]);
            minY = Math.min(minY, positions[i + 1]);
            minZ = Math.min(minZ, positions[i + 2]);
            maxX = Math.max(maxX, positions[i]);
            maxY = Math.max(maxY, positions[i + 1]);
            maxZ = Math.max(maxZ, positions[i + 2]);
        }
        return new Vector3f(
                (minX + maxX) * 0.5f,
                (minY + maxY) * 0.5f,
                (minZ + maxZ) * 0.5f);
    }

    /** Distance from {@code center} to the farthest vertex. */
    private static float computeBoundsRadius(float[] positions, Vector3f center) {
        float maxDistanceSq = 0.0f;
        for (int i = 0; i + 2 < positions.length; i += 3) {
            float dx = positions[i] - center.x();
            float dy = positions[i + 1] - center.y();
            float dz = positions[i + 2] - center.z();
            maxDistanceSq = Math.max(maxDistanceSq, dx * dx + dy * dy + dz * dz);
        }
        return (float) Math.sqrt(maxDistanceSq);
    }
}
