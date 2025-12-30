package com.simplerender.gl;

import com.simplerender.world.ChunkMeshData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GPUMesh {
    private static final Logger logger = LoggerFactory.getLogger(GPUMesh.class);

    private ChunkMeshData lastUpload;
    private final VertexArray vertexArray;
    private final VertexBuffer vertexBuffer;
    private final IndexBuffer indexBuffer;
    private int indexCount;

    public GPUMesh() {
        this.vertexArray = new VertexArray();
        this.vertexBuffer = new VertexBuffer();
        this.indexBuffer = new IndexBuffer();
        this.indexCount = 0;
    }

    public boolean needsUpload(ChunkMeshData snapshot) {
        return snapshot != lastUpload;
    }

    public void upload(ChunkMeshData snapshot) {
        lastUpload = snapshot;
        vertexArray.bind();
        int vertexCount = snapshot.vertexCount();
        if (vertexCount == 0) {
            logger.error("Attempted to upload empty mesh");
            indexCount = 0;
            return;
        }
        vertexBuffer.upload(snapshot.positions(), snapshot.normals(), vertexCount);
        indexBuffer.upload(snapshot.indices(), snapshot.indices().length);
        indexCount = snapshot.indices().length;
    }

    public void draw() {
        if (indexCount == 0) {
            logger.debug("Skipping draw with zero indices");
            return;
        }
        vertexArray.bind();
        indexBuffer.draw(indexCount);
    }
}
