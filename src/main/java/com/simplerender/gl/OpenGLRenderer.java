package com.simplerender.gl;

import com.simplerender.render.culling.FrustumCuller;
import com.simplerender.render.mesh.MeshCache;
import com.simplerender.render.pipeline.RenderPipeline;
import com.simplerender.scene.SceneSnapshot;
import com.simplerender.world.ChunkMeshData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OpenGLRenderer {
    private static final Logger logger = LoggerFactory.getLogger(OpenGLRenderer.class);

    private final RenderPipeline[] pipelines;
    private final GPUMesh[] gpuMeshes;
    private final ShaderProgram shaderProgram;

    public OpenGLRenderer(int chunkCount) {
        this.pipelines = new RenderPipeline[chunkCount];
        this.gpuMeshes = new GPUMesh[chunkCount];
        this.shaderProgram = new ShaderProgram();
        for (int i = 0; i < chunkCount; i++) {
            pipelines[i] = new RenderPipeline(new MeshCache(), new FrustumCuller());
            gpuMeshes[i] = new GPUMesh();
        }
        logger.info("Renderer initialized with {} GPU mesh slots", chunkCount);
    }

    public void render(SceneSnapshot snapshot) {
        if (pipelines.length == 0) {
            logger.error("Renderer has no GPU mesh slots configured");
            return;
        }
        shaderProgram.bind();
        ChunkMeshData[] chunks = snapshot.chunkMeshData();
        int count = Math.min(chunks.length, gpuMeshes.length);
        for (int i = 0; i < count; i++) {
            ChunkMeshData chunk = chunks[i];
            RenderPipeline pipeline = pipelines[i];
            GPUMesh gpuMesh = gpuMeshes[i];
            if (!pipeline.shouldRender(snapshot.camera(), chunk)) {
                continue;
            }
            if (gpuMesh.needsUpload(chunk)) {
                gpuMesh.upload(chunk);
                pipeline.markUploaded(chunk);
                logger.debug("Uploaded chunk {} with {} vertices", i, chunk.vertexCount());
            }
            gpuMesh.draw();
        }
        logger.info("Rendered {} chunks", count);
    }
}
