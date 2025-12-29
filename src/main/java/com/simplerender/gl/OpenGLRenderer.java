package com.simplerender.gl;

import com.simplerender.render.culling.FrustumCuller;
import com.simplerender.render.mesh.MeshCache;
import com.simplerender.render.pipeline.RenderPipeline;
import com.simplerender.scene.SceneSnapshot;

public final class OpenGLRenderer {
    private final RenderPipeline pipeline;
    private final GPUMesh gpuMesh;

    public OpenGLRenderer() {
        this.pipeline = new RenderPipeline(new MeshCache(), new FrustumCuller());
        this.gpuMesh = new GPUMesh();
    }

    public void render(SceneSnapshot snapshot) {
        if (!pipeline.shouldRender(snapshot.camera(), snapshot.chunkMeshData())) {
            return;
        }
        if (gpuMesh.needsUpload(snapshot.chunkMeshData())) {
            gpuMesh.upload(snapshot.chunkMeshData());
            pipeline.markUploaded(snapshot.chunkMeshData());
        }
        gpuMesh.draw();
    }
}
