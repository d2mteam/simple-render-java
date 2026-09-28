package com.simplerender.scene;

import com.simplerender.asset.MaterialData;
import com.simplerender.asset.MeshData;
import java.util.List;

/**
 * Everything the renderer needs for one frame, copied out of the {@link Scene}.
 *
 * <p>The renderer only ever sees snapshots, never the live scene, so the UI can keep
 * editing the scene while a frame is being drawn.
 */
public record SceneSnapshot(CameraSnapshot camera, List<Light> lights, List<RenderItem> items) {

    /** A mesh + material to draw with a fixed model matrix (column-major). */
    public record RenderItem(MeshData mesh, MaterialData material, float[] modelMatrix) {
    }
}
