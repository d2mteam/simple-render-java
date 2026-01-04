package com.simplerender.render;

import com.simplerender.math.Vector3f;

public final class Transform {
    private final float[] baseMatrix;
    private float x;
    private float y;
    private float z;
    private float scale = 1.0f;

    public Transform() {
        this.baseMatrix = null;
    }

    public Transform(float[] baseMatrix) {
        this.baseMatrix = baseMatrix == null ? null : java.util.Arrays.copyOf(baseMatrix, baseMatrix.length);
    }

    public synchronized void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public synchronized void setScale(float scale) {
        this.scale = scale;
    }

    public synchronized Vector3f position() {
        return new Vector3f(x, y, z);
    }

    public synchronized float scale() {
        return scale;
    }

    public synchronized float[] matrix() {
        float[] local = new float[16];
        local[0] = scale;
        local[5] = scale;
        local[10] = scale;
        local[12] = x;
        local[13] = y;
        local[14] = z;
        local[15] = 1.0f;
        if (baseMatrix == null) {
            return local;
        }
        return com.simplerender.math.Matrix4f.multiply(baseMatrix, local);
    }
}
