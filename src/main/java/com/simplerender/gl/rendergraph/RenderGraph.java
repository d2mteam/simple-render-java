package com.simplerender.gl.rendergraph;

import java.util.ArrayList;
import java.util.List;

/**
 * Ordered list of render passes executed each frame.
 *
 * <p>Acts as a simple render graph builder that can be specialized for
 * different pipelines (e.g., with or without ray tracing).
 */
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
