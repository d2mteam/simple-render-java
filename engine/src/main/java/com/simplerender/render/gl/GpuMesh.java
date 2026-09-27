package com.simplerender.render.gl;

import com.simplerender.asset.MeshData;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * A mesh uploaded to the GPU: one interleaved vertex buffer, one index buffer and the
 * vertex array object that describes them.
 *
 * <p>Vertex layout (attribute location: contents):
 * <pre>
 *   0: position   vec3      3: uv1        vec2
 *   1: normal     vec3      4: tangent    vec3
 *   2: uv0        vec2      5: bitangent  vec3
 * </pre>
 */
public final class GpuMesh implements AutoCloseable {
    private static final int[] ATTRIBUTE_SIZES = { 3, 3, 2, 2, 3, 3 };
    private static final int FLOATS_PER_VERTEX = 16;

    private final int vertexArray;
    private final int vertexBuffer;
    private final int indexBuffer;
    private final int indexCount;

    public GpuMesh(MeshData mesh) {
        int[] indices = mesh.indices();
        indexCount = indices.length;

        vertexArray = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vertexArray);

        vertexBuffer = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, interleave(mesh), GL15.GL_STATIC_DRAW);

        indexBuffer = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STATIC_DRAW);

        int stride = FLOATS_PER_VERTEX * Float.BYTES;
        int offset = 0;
        for (int location = 0; location < ATTRIBUTE_SIZES.length; location++) {
            GL20.glEnableVertexAttribArray(location);
            GL20.glVertexAttribPointer(location, ATTRIBUTE_SIZES[location], GL11.GL_FLOAT, false, stride,
                    (long) offset * Float.BYTES);
            offset += ATTRIBUTE_SIZES[location];
        }
        GL30.glBindVertexArray(0);
    }

    public void draw() {
        if (indexCount == 0) {
            return;
        }
        GL30.glBindVertexArray(vertexArray);
        GL11.glDrawElements(GL11.GL_TRIANGLES, indexCount, GL11.GL_UNSIGNED_INT, 0);
        GL30.glBindVertexArray(0);
    }

    @Override
    public void close() {
        GL30.glDeleteVertexArrays(vertexArray);
        GL15.glDeleteBuffers(vertexBuffer);
        GL15.glDeleteBuffers(indexBuffer);
    }

    /** Packs the separate attribute arrays into one array in the layout above. */
    private static float[] interleave(MeshData mesh) {
        float[][] attributes = {
                mesh.positions(), mesh.normals(), mesh.texCoords0(),
                mesh.texCoords1(), mesh.tangents(), mesh.bitangents(),
        };
        int vertexCount = mesh.vertexCount();
        float[] data = new float[vertexCount * FLOATS_PER_VERTEX];
        int d = 0;
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            for (int a = 0; a < attributes.length; a++) {
                int size = ATTRIBUTE_SIZES[a];
                System.arraycopy(attributes[a], vertex * size, data, d, size);
                d += size;
            }
        }
        return data;
    }
}
