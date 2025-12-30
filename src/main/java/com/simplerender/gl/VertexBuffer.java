package com.simplerender.gl;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;

final class VertexBuffer {
    private int bufferId;
    private int capacityFloats;
    private FloatBuffer floatBuffer;

    public void init() {
        bufferId = GL15.glGenBuffers();
    }

    public void bind() {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bufferId);
    }

    public void ensureCapacity(int floatCount) {
        if (floatCount <= capacityFloats) {
            return;
        }
        capacityFloats = floatCount;
        floatBuffer = BufferUtils.createFloatBuffer(capacityFloats);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) capacityFloats * Float.BYTES, GL15.GL_DYNAMIC_DRAW);
    }

    public void upload(float[] data, int floatCount) {
        bind();
        ensureCapacity(floatCount);
        floatBuffer.clear();
        floatBuffer.put(data, 0, floatCount).flip();
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, floatBuffer);
    }
}
