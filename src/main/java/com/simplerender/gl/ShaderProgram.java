package com.simplerender.gl;

final class ShaderProgram {
    private boolean bound;

    public void bind() {
        bound = true;
    }

    public boolean isBound() {
        return bound;
    }
}
