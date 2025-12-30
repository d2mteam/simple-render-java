package com.simplerender.gl;

import com.simplerender.render.culling.FrustumCuller;
import com.simplerender.render.mesh.MeshCache;
import com.simplerender.render.pipeline.RenderPipeline;
import com.simplerender.scene.SceneSnapshot;
import com.simplerender.world.ChunkMeshData;

public final class OpenGLRenderer {
    private final RenderPipeline[] pipelines;
    private final GPUMesh[] gpuMeshes;

    public OpenGLRenderer(int chunkCount) {
        this.pipelines = new RenderPipeline[chunkCount];
        this.gpuMeshes = new GPUMesh[chunkCount];
        for (int i = 0; i < chunkCount; i++) {
            pipelines[i] = new RenderPipeline(new MeshCache(), new FrustumCuller());
            gpuMeshes[i] = new GPUMesh();
        }
    }

    public void render(SceneSnapshot snapshot) {
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
            }
            gpuMesh.draw();
        }
    }
}
