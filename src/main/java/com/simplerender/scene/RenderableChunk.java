package com.simplerender.scene;

import com.simplerender.render.RenderItem;

public final class RenderableChunk {
    private RenderItem renderItem;

    public RenderableChunk(RenderItem renderItem) {
        this.renderItem = renderItem;
    }

    public void updateRenderItem(RenderItem renderItem) {
        this.renderItem = renderItem;
    }

    public RenderItem snapshot() {
        return renderItem;
    }
}
