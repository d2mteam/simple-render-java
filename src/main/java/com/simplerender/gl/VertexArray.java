package com.simplerender.gl;

import org.lwjgl.opengl.GL30;

/**
 * Lightweight wrapper around an OpenGL Vertex Array Object.
 *
 * <p>Encapsulates creation and binding for mesh draw calls.
 */
final class VertexArray {
    private int vaoId;

    public void init() {
        vaoId = GL30.glGenVertexArrays();
    }

    public void bind() {
        GL30.glBindVertexArray(vaoId);
    }

    public int id() {
        return vaoId;
    }
}
