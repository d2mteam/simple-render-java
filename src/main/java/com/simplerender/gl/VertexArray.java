package com.simplerender.gl;

final class VertexArray {
    private boolean bound;

    public void bind() {
        bound = true;
    }

    public boolean isBound() {
        return bound;
    }
}
