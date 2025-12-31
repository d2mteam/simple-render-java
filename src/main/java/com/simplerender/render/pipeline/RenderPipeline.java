package com.simplerender.render.pipeline;

import com.simplerender.asset.MeshData;
import com.simplerender.math.Vector3f;
import com.simplerender.render.RenderItem;
import com.simplerender.render.Transform;
import com.simplerender.render.culling.FrustumCuller;

public final class RenderPipeline {
    private final FrustumCuller frustumCuller;

    public RenderPipeline(FrustumCuller frustumCuller) {
        this.frustumCuller = frustumCuller;
    }

    public void updateFrustum(float[] projectionMatrix, float[] viewMatrix) {
        frustumCuller.update(projectionMatrix, viewMatrix);
    }

    public boolean shouldRender(RenderItem item, MeshData meshData) {
        if (meshData == null || meshData.vertexCount() == 0) {
            return true;
        }
        Transform transform = item.transform();
        Vector3f worldCenter = transform.position().add(meshData.boundsCenter().scale(transform.scale()));
        float radius = meshData.boundsRadius() * transform.scale();
        return frustumCuller.isVisible(worldCenter, radius);
    }
}
