package com.simplerender.render.pipeline;

import com.simplerender.camera.CameraSnapshot;
import com.simplerender.render.culling.FrustumCuller;

public final class RenderPipeline {
    private final FrustumCuller frustumCuller;

    public RenderPipeline(FrustumCuller frustumCuller) {
        this.frustumCuller = frustumCuller;
    }

    public boolean shouldRender(CameraSnapshot cameraSnapshot) {
        return frustumCuller.isVisible(cameraSnapshot);
    }
}
