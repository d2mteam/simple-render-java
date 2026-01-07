package com.simplerender.render;

import java.util.Objects;

/**
 * Stable identifier for a GPU texture resource.
 */
public final class TextureHandle {
    private final int id;

    public TextureHandle(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TextureHandle textureHandle)) {
            return false;
        }
        return id == textureHandle.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
