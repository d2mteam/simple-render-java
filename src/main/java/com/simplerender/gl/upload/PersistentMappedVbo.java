package com.simplerender.gl.upload;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL44;

public final class PersistentMappedVbo {
    private final int bufferId;
    private int capacityBytes;
    private ByteBuffer mappedBuffer;

    public PersistentMappedVbo() {
        this.bufferId = GL15.glGenBuffers();
    }

    public void bind() {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bufferId);
    }

    public int id() {
        return bufferId;
    }

    public void ensureCapacity(int byteCount) {
        if (byteCount <= capacityBytes) {
            return;
        }
        capacityBytes = byteCount;
        bind();
        if (GL.getCapabilities().OpenGL44) {
            int flags = GL44.GL_MAP_WRITE_BIT | GL44.GL_MAP_PERSISTENT_BIT | GL44.GL_MAP_COHERENT_BIT;
            GL44.glBufferStorage(GL15.GL_ARRAY_BUFFER, capacityBytes, flags);
            mappedBuffer = GL30.glMapBufferRange(GL15.GL_ARRAY_BUFFER, 0, capacityBytes, flags, mappedBuffer);
        } else {
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, capacityBytes, GL15.GL_DYNAMIC_DRAW);
            mappedBuffer = null;
        }
    }

    public void upload(float[] data) {
        int byteCount = data.length * Float.BYTES;
        ensureCapacity(byteCount);
        if (mappedBuffer != null) {
            FloatBuffer floatBuffer = mappedBuffer.asFloatBuffer();
            floatBuffer.clear();
            floatBuffer.put(data).flip();
        } else {
            FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(data.length);
            floatBuffer.put(data).flip();
            bind();
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, floatBuffer);
        }
    }
}
