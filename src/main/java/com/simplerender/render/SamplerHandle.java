package com.simplerender.render;

/**
 * Stable identifier for a GPU sampler resource.
 */
public final class SamplerHandle {
    private final int id;

    public SamplerHandle(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SamplerHandle samplerHandle)) {
            return false;
        }
        return id == samplerHandle.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
