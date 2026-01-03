package com.simplerender.render.pipeline;

import com.simplerender.asset.MeshData;
import com.simplerender.math.Vector3f;
import com.simplerender.render.RenderItem;
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
        if (item == null || meshData == null) {
            return true;
        }
        Vector3f center = meshData.boundsCenter();
        float radius = meshData.boundsRadius();
        if (center == null || radius <= 0.0f) {
            return true;
        }
        Vector3f position = item.transform().position();
        float scale = Math.abs(item.transform().scale());
        Vector3f worldCenter = new Vector3f(
            center.x() * scale + position.x(),
            center.y() * scale + position.y(),
            center.z() * scale + position.z()
        );
        return frustumCuller.isVisible(worldCenter, radius * scale);
    }
}
