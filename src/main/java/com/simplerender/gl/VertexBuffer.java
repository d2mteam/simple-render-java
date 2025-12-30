package com.simplerender.gl;

final class VertexBuffer {
    private float[] positions;
    private float[] normals;
    private int vertexCount;

    public void upload(float[] positions, float[] normals, int vertexCount) {
        this.positions = positions;
        this.normals = normals;
        this.vertexCount = vertexCount;
    }

    public int vertexCount() {
        return vertexCount;
    }
}
