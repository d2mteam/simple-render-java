package com.simplerender.render.pipeline;

import com.simplerender.camera.CameraSnapshot;
import com.simplerender.render.culling.FrustumCuller;
import com.simplerender.render.mesh.MeshCache;
import com.simplerender.world.ChunkMeshData;

public final class RenderPipeline {
    private final MeshCache meshCache;
    private final FrustumCuller frustumCuller;

    public RenderPipeline(MeshCache meshCache, FrustumCuller frustumCuller) {
        this.meshCache = meshCache;
        this.frustumCuller = frustumCuller;
    }

    public boolean shouldRender(CameraSnapshot cameraSnapshot, ChunkMeshData chunkMeshData) {
        if (!frustumCuller.isVisible(cameraSnapshot)) {
            return false;
        }
        return chunkMeshData.vertexCount() > 0 || meshCache.needsUpload(chunkMeshData);
    }

    public void markUploaded(ChunkMeshData chunkMeshData) {
        meshCache.updateSnapshot(chunkMeshData);
    }
}
