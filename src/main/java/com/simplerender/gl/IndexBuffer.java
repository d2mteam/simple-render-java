package com.simplerender.gl;

final class IndexBuffer {
    private int[] indices;
    private int indexCount;

    public void upload(int[] indices, int indexCount) {
        this.indices = indices;
        this.indexCount = indexCount;
    }

    public void draw(int count) {
        if (indices == null || count <= 0) {
            return;
        }
        indexCount = count;
    }

    public int indexCount() {
        return indexCount;
    }
}
