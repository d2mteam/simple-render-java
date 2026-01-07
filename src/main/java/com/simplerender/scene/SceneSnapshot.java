package com.simplerender.scene;

import com.simplerender.camera.CameraSnapshot;
import com.simplerender.render.RenderItem;

/**
 * Immutable snapshot of the scene for the render thread.
 *
 * <p>Snapshots decouple simulation updates from rendering by packaging the
 * camera state and render items into a read-only payload.
 */
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
