package com.simplerender.gl;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;

final class VertexBuffer {
    private int bufferId;
    private int capacityFloats;
    private FloatBuffer floatBuffer;
    private int usage = GL15.GL_DYNAMIC_DRAW;

    public void init() {
        bufferId = GL15.glGenBuffers();
    }

    public void bind() {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bufferId);
    }

    public void ensureCapacity(int floatCount, int usage) {
        if (floatCount <= capacityFloats && this.usage == usage) {
            return;
        }
        capacityFloats = floatCount;
        this.usage = usage;
        floatBuffer = BufferUtils.createFloatBuffer(capacityFloats);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) capacityFloats * Float.BYTES, usage);
    }

    public void upload(float[] data, int floatCount) {
        upload(data, floatCount, GL15.GL_DYNAMIC_DRAW);
    }

    public void upload(float[] data, int floatCount, int usage) {
        bind();
        ensureCapacity(floatCount, usage);
        floatBuffer.clear();
        floatBuffer.put(data, 0, floatCount).flip();
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, floatBuffer);
    }
}
