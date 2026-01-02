package com.simplerender.gl;

import org.lwjgl.opengl.GL15;

public enum MeshUploadMode {
    STATIC_ONE_SHOT(GL15.GL_STATIC_DRAW),
    DYNAMIC_PERSISTENT(GL15.GL_DYNAMIC_DRAW);

    private final int bufferUsage;

    MeshUploadMode(int bufferUsage) {
        this.bufferUsage = bufferUsage;
    }

    public int bufferUsage() {
        return bufferUsage;
    }
}
