package com.simplerender.gl;

import com.simplerender.asset.MeshData;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GPUMesh {
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
        upload(snapshot, MeshUploadMode.STATIC_ONE_SHOT);
    }

    public void upload(MeshData snapshot, MeshUploadMode uploadMode) {
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
        float[] texCoords = snapshot.texCoords();
        float[] tangents = snapshot.tangents();
        float[] bitangents = snapshot.bitangents();
        int required = positions.length / 3 * 14;
        ensureInterleavedCapacity(required);
        interleave(positions, normals, texCoords, tangents, bitangents, interleaved);
        vertexBuffer.upload(interleaved, required, uploadMode.bufferUsage());
        int[] indexData = snapshot.indices();
        indexBuffer.upload(indexData, indexData.length, uploadMode.bufferUsage());
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 14 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 14 * Float.BYTES, 3L * Float.BYTES);
        GL20.glEnableVertexAttribArray(2);
        GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, 14 * Float.BYTES, 6L * Float.BYTES);
        GL20.glEnableVertexAttribArray(3);
        GL20.glVertexAttribPointer(3, 3, GL11.GL_FLOAT, false, 14 * Float.BYTES, 8L * Float.BYTES);
        GL20.glEnableVertexAttribArray(4);
        GL20.glVertexAttribPointer(4, 3, GL11.GL_FLOAT, false, 14 * Float.BYTES, 11L * Float.BYTES);
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

    public MeshData snapshot() {
        return lastUpload;
    }

    private void ensureInterleavedCapacity(int required) {
        if (interleaved == null || interleaved.length < required) {
            interleaved = new float[required];
        }
    }

    private void interleave(
        float[] positions,
        float[] normals,
        float[] texCoords,
        float[] tangents,
        float[] bitangents,
        float[] data
    ) {
        int vertexCount = positions.length / 3;
        int p = 0;
        int n = 0;
        int t = 0;
        int tan = 0;
        int bitan = 0;
        int d = 0;
        for (int i = 0; i < vertexCount; i++) {
            data[d++] = positions[p++];
            data[d++] = positions[p++];
            data[d++] = positions[p++];
            data[d++] = normals[n++];
            data[d++] = normals[n++];
            data[d++] = normals[n++];
            data[d++] = texCoords[t++];
            data[d++] = texCoords[t++];
            data[d++] = tangents[tan++];
            data[d++] = tangents[tan++];
            data[d++] = tangents[tan++];
            data[d++] = bitangents[bitan++];
            data[d++] = bitangents[bitan++];
            data[d++] = bitangents[bitan++];
        }
    }
}
