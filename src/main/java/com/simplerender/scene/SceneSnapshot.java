package com.simplerender.scene;

import com.simplerender.camera.CameraSnapshot;
import com.simplerender.render.RenderItem;

public final class SceneSnapshot {
    private final CameraSnapshot camera;
    private final RenderItem[] renderItems;

    public SceneSnapshot(CameraSnapshot camera, RenderItem[] renderItems) {
        this.camera = camera;
        this.renderItems = renderItems;
    }

    public CameraSnapshot camera() {
        return camera;
    }

    public RenderItem[] renderItems() {
        return renderItems;
    }
}
