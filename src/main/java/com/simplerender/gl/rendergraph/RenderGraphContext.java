package com.simplerender.gl.rendergraph;

import com.simplerender.gl.OpenGLRenderer;
import com.simplerender.scene.SceneSnapshot;

/**
 * Per-frame context passed through render graph passes.
 *
 * <p>Provides access to the active renderer and the immutable scene snapshot.
 */
public final class RenderGraphContext {
    private final OpenGLRenderer renderer;
    private final SceneSnapshot snapshot;

    public RenderGraphContext(OpenGLRenderer renderer, SceneSnapshot snapshot) {
        this.renderer = renderer;
        this.snapshot = snapshot;
    }

    public OpenGLRenderer renderer() {
        return renderer;
    }

    public SceneSnapshot snapshot() {
        return snapshot;
    }
}
