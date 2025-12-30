package com.simplerender.world;

public final class ChunkMeshData {
    private final int vertexCount;
    private final float[] positions;
    private final float[] normals;
    private final int[] indices;

    public ChunkMeshData(int vertexCount, float[] positions, float[] normals, int[] indices) {
        this.vertexCount = vertexCount;
        this.positions = positions;
        this.normals = normals;
        this.indices = indices;
    }

    public int vertexCount() {
        return vertexCount;
    }

    public float[] positions() {
        return positions;
    }

    public float[] normals() {
        return normals;
    }

    public int[] indices() {
        return indices;
    }

    public static ChunkMeshData empty() {
        return new ChunkMeshData(0, new float[0], new float[0], new int[0]);
    }
}
