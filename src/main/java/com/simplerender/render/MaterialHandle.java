package com.simplerender.render;

import java.util.Objects;

/**
 * Stable identifier for a GPU material binding.
 */
public final class MaterialHandle {
    private final int id;

    public MaterialHandle(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MaterialHandle materialHandle)) {
            return false;
        }
        return id == materialHandle.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
