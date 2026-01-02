package com.simplerender.gl.rendergraph;

public interface RenderPass {
    String name();

    void execute(RenderGraphContext context);
}
