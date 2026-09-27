package com.simplerender.render.gl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Two triangles covering the whole viewport, used by full-screen passes.
 * Attribute 0 is the clip-space position, attribute 1 the UV (0..1).
 */
public final class FullscreenQuad implements AutoCloseable {
    private static final float[] VERTICES = {
            // x, y, u, v
            -1.0f, -1.0f, 0.0f, 0.0f,
            1.0f, -1.0f, 1.0f, 0.0f,
            1.0f, 1.0f, 1.0f, 1.0f,
            -1.0f, -1.0f, 0.0f, 0.0f,
            1.0f, 1.0f, 1.0f, 1.0f,
            -1.0f, 1.0f, 0.0f, 1.0f,
    };

    private final int vertexArray;
    private final int vertexBuffer;

    public FullscreenQuad() {
        vertexArray = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vertexArray);
        vertexBuffer = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, VERTICES, GL15.GL_STATIC_DRAW);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 2L * Float.BYTES);
        GL30.glBindVertexArray(0);
    }

    public void draw() {
        GL30.glBindVertexArray(vertexArray);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);
    }

    @Override
    public void close() {
        GL30.glDeleteVertexArrays(vertexArray);
        GL15.glDeleteBuffers(vertexBuffer);
    }
}
