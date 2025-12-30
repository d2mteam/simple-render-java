package com.simplerender.render;

public final class RenderItem {
    private final MeshHandle meshHandle;
    private final MaterialHandle materialHandle;

    public RenderItem(MeshHandle meshHandle, MaterialHandle materialHandle) {
        this.meshHandle = meshHandle;
        this.materialHandle = materialHandle;
    }

    public MeshHandle meshHandle() {
        return meshHandle;
    }

    public MaterialHandle materialHandle() {
        return materialHandle;
    }
}
