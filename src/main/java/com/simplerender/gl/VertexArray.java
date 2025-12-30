package com.simplerender.gl;

import org.lwjgl.opengl.GL30;

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
