package com.simplerender.gl;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;

import java.nio.IntBuffer;

final class IndexBuffer {
    private int bufferId;
    private int capacityIndices;
    private IntBuffer intBuffer;
    private int usage = GL15.GL_DYNAMIC_DRAW;

    public void init() {
        bufferId = GL15.glGenBuffers();
    }

    public void bind() {
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, bufferId);
    }

    public void ensureCapacity(int indexCount, int usage) {
        if (indexCount <= capacityIndices && this.usage == usage) {
            return;
        }
        capacityIndices = indexCount;
        this.usage = usage;
        intBuffer = BufferUtils.createIntBuffer(capacityIndices);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, (long) capacityIndices * Integer.BYTES, usage);
    }

    public void upload(int[] indices, int indexCount) {
        upload(indices, indexCount, GL15.GL_DYNAMIC_DRAW);
    }

    public void upload(int[] indices, int indexCount, int usage) {
        bind();
        ensureCapacity(indexCount, usage);
        intBuffer.clear();
        intBuffer.put(indices, 0, indexCount).flip();
        GL15.glBufferSubData(GL15.GL_ELEMENT_ARRAY_BUFFER, 0, intBuffer);
    }
}
