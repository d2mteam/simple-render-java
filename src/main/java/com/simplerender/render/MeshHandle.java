package com.simplerender.render;

import java.util.Objects;

/**
 * Stable identifier for a GPU mesh resource.
 */
public final class MeshHandle {
    private final int id;

    public MeshHandle(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MeshHandle meshHandle)) {
            return false;
        }
        return id == meshHandle.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
