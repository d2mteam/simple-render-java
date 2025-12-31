package com.simplerender.render.pipeline;

import com.simplerender.asset.MeshData;
import com.simplerender.render.RenderItem;
import com.simplerender.render.culling.FrustumCuller;

public final class RenderPipeline {
    private final FrustumCuller frustumCuller;

    public RenderPipeline(FrustumCuller frustumCuller) {
        this.frustumCuller = frustumCuller;
    }

    public void updateFrustum(float[] projectionMatrix, float[] viewMatrix) {
        // Frustum culling disabled to avoid dropping non-spherical meshes.
    }

    public boolean shouldRender(RenderItem item, MeshData meshData) {
        return true;
    }
}
