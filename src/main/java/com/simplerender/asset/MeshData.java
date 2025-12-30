package com.simplerender.asset;

import java.util.Arrays;

public final class MeshData {
    private final float[] positions;
    private final float[] normals;
    private final int[] indices;

    public MeshData(float[] positions, float[] normals, int[] indices) {
        this.positions = Arrays.copyOf(positions, positions.length);
        this.normals = Arrays.copyOf(normals, normals.length);
        this.indices = Arrays.copyOf(indices, indices.length);
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
}
