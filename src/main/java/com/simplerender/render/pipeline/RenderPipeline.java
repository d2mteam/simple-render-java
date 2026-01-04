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
        float[] modelMatrix = item.transform().matrix();
        float worldX = modelMatrix[0] * center.x()
            + modelMatrix[4] * center.y()
            + modelMatrix[8] * center.z()
            + modelMatrix[12];
        float worldY = modelMatrix[1] * center.x()
            + modelMatrix[5] * center.y()
            + modelMatrix[9] * center.z()
            + modelMatrix[13];
        float worldZ = modelMatrix[2] * center.x()
            + modelMatrix[6] * center.y()
            + modelMatrix[10] * center.z()
            + modelMatrix[14];
        Vector3f worldCenter = new Vector3f(worldX, worldY, worldZ);
        float scaleX = (float) Math.sqrt(
            modelMatrix[0] * modelMatrix[0]
                + modelMatrix[1] * modelMatrix[1]
                + modelMatrix[2] * modelMatrix[2]
        );
        float scaleY = (float) Math.sqrt(
            modelMatrix[4] * modelMatrix[4]
                + modelMatrix[5] * modelMatrix[5]
                + modelMatrix[6] * modelMatrix[6]
        );
        float scaleZ = (float) Math.sqrt(
            modelMatrix[8] * modelMatrix[8]
                + modelMatrix[9] * modelMatrix[9]
                + modelMatrix[10] * modelMatrix[10]
        );
        float maxScale = Math.max(scaleX, Math.max(scaleY, scaleZ));
        return frustumCuller.isVisible(worldCenter, radius * maxScale);
    }
}
