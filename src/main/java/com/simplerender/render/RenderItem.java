package com.simplerender.render;

public final class RenderItem {
    private final MeshHandle meshHandle;
    private final MaterialHandle materialHandle;
    private final Transform transform;

    public RenderItem(MeshHandle meshHandle, MaterialHandle materialHandle, Transform transform) {
        this.meshHandle = meshHandle;
        this.materialHandle = materialHandle;
        this.transform = transform;
    }

    public MeshHandle meshHandle() {
        return meshHandle;
    }

    public MaterialHandle materialHandle() {
        return materialHandle;
    }

    public Transform transform() {
        return transform;
    }
}
