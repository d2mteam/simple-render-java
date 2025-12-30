package com.simplerender.gl;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;

import java.nio.IntBuffer;

final class IndexBuffer {
    private int bufferId;
    private int capacityIndices;
    private IntBuffer intBuffer;

    public void init() {
        bufferId = GL15.glGenBuffers();
    }

    public void bind() {
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, bufferId);
    }

    public void ensureCapacity(int indexCount) {
        if (indexCount <= capacityIndices) {
            return;
        }
        capacityIndices = indexCount;
        intBuffer = BufferUtils.createIntBuffer(capacityIndices);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, (long) capacityIndices * Integer.BYTES, GL15.GL_DYNAMIC_DRAW);
    }

    public void upload(int[] indices, int indexCount) {
        bind();
        ensureCapacity(indexCount);
        intBuffer.clear();
        intBuffer.put(indices, 0, indexCount).flip();
        GL15.glBufferSubData(GL15.GL_ELEMENT_ARRAY_BUFFER, 0, intBuffer);
    }
}
