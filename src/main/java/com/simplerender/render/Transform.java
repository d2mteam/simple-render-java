package com.simplerender.render;

public final class Transform {
    private float x;
    private float y;
    private float z;
    private float scale = 1.0f;

    public synchronized void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public synchronized void setScale(float scale) {
        this.scale = scale;
    }

    public synchronized float[] matrix() {
        float[] matrix = new float[16];
        matrix[0] = scale;
        matrix[5] = scale;
        matrix[10] = scale;
        matrix[12] = x;
        matrix[13] = y;
        matrix[14] = z;
        matrix[15] = 1.0f;
        return matrix;
    }
}
