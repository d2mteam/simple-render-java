package com.simplerender.gl.rendergraph;

import java.util.ArrayList;
import java.util.List;

public final class RenderGraph {
    private final List<RenderPass> passes = new ArrayList<>();

    public RenderGraph addPass(RenderPass pass) {
        passes.add(pass);
        return this;
    }

    public void execute(RenderGraphContext context) {
        for (RenderPass pass : passes) {
            pass.execute(context);
        }
    }
}
