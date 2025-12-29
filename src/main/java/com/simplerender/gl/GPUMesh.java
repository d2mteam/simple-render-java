package com.simplerender.gl;

import com.simplerender.world.ChunkMeshData;

public final class GPUMesh {
    private ChunkMeshData lastUpload;
    private final VertexArray vertexArray;
    private final VertexBuffer vertexBuffer;
    private final IndexBuffer indexBuffer;

    public GPUMesh() {
        this.vertexArray = new VertexArray();
        this.vertexBuffer = new VertexBuffer();
        this.indexBuffer = new IndexBuffer();
    }

    public boolean needsUpload(ChunkMeshData snapshot) {
        return snapshot != lastUpload;
    }

    public void upload(ChunkMeshData snapshot) {
        lastUpload = snapshot;
        vertexArray.bind();
        vertexBuffer.upload(snapshot.positions(), snapshot.normals());
        indexBuffer.upload(snapshot.indices());
    }

    public void draw() {
        vertexArray.bind();
        indexBuffer.draw();
    }
}
