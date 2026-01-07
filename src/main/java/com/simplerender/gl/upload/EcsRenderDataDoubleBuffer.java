package com.simplerender.gl.upload;

import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL31;

/**
 * Double-buffered UBO/VBO uploader for ECS-like render data.
 *
 * <p>Alternates between two buffers to avoid CPU/GPU sync stalls when updating.
 */
public final class EcsRenderDataDoubleBuffer {
    private final int[] buffers;
    private final int target;
    private int capacityBytes;
    private int writeIndex;

    public EcsRenderDataDoubleBuffer(int initialBytes) {
        this(GL31.GL_UNIFORM_BUFFER, initialBytes);
    }

    public EcsRenderDataDoubleBuffer(int target, int initialBytes) {
        this.target = target;
        this.buffers = new int[] { GL15.glGenBuffers(), GL15.glGenBuffers() };
        this.capacityBytes = Math.max(initialBytes, 1);
        allocateBuffers();
    }

    public void update(ByteBuffer data) {
        int required = Math.max(data.remaining(), 1);
        if (required > capacityBytes) {
            capacityBytes = required;
            allocateBuffers();
        }
        int bufferId = buffers[writeIndex];
        GL15.glBindBuffer(target, bufferId);
        GL15.glBufferSubData(target, 0, data);
        writeIndex = 1 - writeIndex;
    }

    public int currentBufferId() {
        return buffers[1 - writeIndex];
    }

    private void allocateBuffers() {
        ByteBuffer empty = BufferUtils.createByteBuffer(capacityBytes);
        for (int bufferId : buffers) {
            GL15.glBindBuffer(target, bufferId);
            GL15.glBufferData(target, empty, GL15.GL_DYNAMIC_DRAW);
        }
    }
}
