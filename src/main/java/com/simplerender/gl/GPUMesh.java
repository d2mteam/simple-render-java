package com.simplerender.gl;

import com.simplerender.asset.MeshData;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GPUMesh {
    private static final Logger logger = LoggerFactory.getLogger(GPUMesh.class);

    private MeshData lastUpload;
    private final VertexArray vertexArray;
    private final VertexBuffer vertexBuffer;
    private final IndexBuffer indexBuffer;
    private boolean initialized;
    private int indexCount;
    private float[] interleaved;

    public GPUMesh() {
        this.vertexArray = new VertexArray();
        this.vertexBuffer = new VertexBuffer();
        this.indexBuffer = new IndexBuffer();
        this.indexCount = 0;
    }

    public boolean needsUpload(MeshData snapshot) {
        return snapshot != lastUpload;
    }

    public void upload(MeshData snapshot) {
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
        float[] positions = snapshot.positions();
        float[] normals = snapshot.normals();
        int required = positions.length / 3 * 6;
        ensureInterleavedCapacity(required);
        interleave(positions, normals, interleaved);
        vertexBuffer.upload(interleaved, required);
        int[] indexData = snapshot.indices();
        indexBuffer.upload(indexData, indexData.length);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 6 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 6 * Float.BYTES, 3L * Float.BYTES);
        indexCount = indexData.length;
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

    private void ensureInterleavedCapacity(int required) {
        if (interleaved == null || interleaved.length < required) {
            interleaved = new float[required];
        }
    }

    private void interleave(float[] positions, float[] normals, float[] data) {
        int vertexCount = positions.length / 3;
        int p = 0;
        int n = 0;
        int d = 0;
        for (int i = 0; i < vertexCount; i++) {
            data[d++] = positions[p++];
            data[d++] = positions[p++];
            data[d++] = positions[p++];
            data[d++] = normals[n++];
            data[d++] = normals[n++];
            data[d++] = normals[n++];
        }
    }
}
