package com.simplerender.gl;

import com.simplerender.world.ChunkMeshData;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GPUMesh {
    private static final Logger logger = LoggerFactory.getLogger(GPUMesh.class);

    private ChunkMeshData lastUpload;
    private final VertexArray vertexArray;
    private final VertexBuffer vertexBuffer;
    private final IndexBuffer indexBuffer;
    private boolean initialized;
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
        int vertexCount = snapshot.vertexCount();
        if (vertexCount == 0) {
            logger.error("Attempted to upload empty mesh");
            indexCount = 0;
            return;
        }
        if (!initialized) {
            vertexArray.init();
            vertexBuffer.init();
            indexBuffer.init();
            initialized = true;
        }
        vertexArray.bind();
        vertexBuffer.bind();
        indexBuffer.bind();
        vertexBuffer.upload(snapshot.positions(), snapshot.positions().length);
        indexBuffer.upload(snapshot.indices(), snapshot.indices().length);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        indexCount = snapshot.indices().length;
        logger.debug("Uploaded mesh with {} vertices and {} indices", vertexCount, indexCount);
    }

    public void draw() {
        if (!initialized || indexCount == 0) {
            logger.debug("Skipping draw with zero indices");
            return;
        }
        vertexArray.bind();
        GL11.glDrawElements(GL11.GL_TRIANGLES, indexCount, GL11.GL_UNSIGNED_INT, 0);
    }
}
