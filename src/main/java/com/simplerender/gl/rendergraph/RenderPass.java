package com.simplerender.gl.rendergraph;

/**
 * Contract for a single render graph pass.
 *
 * <p>Implementations are executed sequentially and may issue draw or compute
 * calls using the provided {@link RenderGraphContext}.
 */
public interface RenderPass {
    String name();

    void execute(RenderGraphContext context);
}
