package com.simplerender.render.mesh;

public final class MeshData {
    private final float[] positions;
    private final float[] normals;
    private final int[] indices;
    private int vertexCount;
    private int indexCount;

    public MeshData(int maxVertices, int maxIndices) {
        this.positions = new float[maxVertices * 3];
        this.normals = new float[maxVertices * 3];
        this.indices = new int[maxIndices];
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

    public int vertexCount() {
        return vertexCount;
    }

    public int indexCount() {
        return indexCount;
    }

    public void setCounts(int vertexCount, int indexCount) {
        this.vertexCount = vertexCount;
        this.indexCount = indexCount;
    }
}
