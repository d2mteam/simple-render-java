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

    public void updateTransform(float x, float y, float z, float scale) {
        renderItem.transform().setPosition(x, y, z);
        renderItem.transform().setScale(scale);
    }

    public RenderItem snapshot() {
        return renderItem;
    }
}
