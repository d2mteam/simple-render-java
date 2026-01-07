package com.simplerender.render.buffer;

/**
 * Scratch buffer for temporary render math and data packing.
 */
public final class RenderBuffer {
    private final float[] scratch;

    public RenderBuffer(int size) {
        this.scratch = new float[size];
    }

    public float[] scratch() {
        return scratch;
    }
}
