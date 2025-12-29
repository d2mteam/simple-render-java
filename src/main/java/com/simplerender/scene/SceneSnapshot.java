package com.simplerender.scene;

import com.simplerender.camera.CameraSnapshot;
import com.simplerender.world.ChunkMeshData;

public final class SceneSnapshot {
    private final CameraSnapshot camera;
    private final ChunkMeshData chunkMeshData;

    public SceneSnapshot(CameraSnapshot camera, ChunkMeshData chunkMeshData) {
        this.camera = camera;
        this.chunkMeshData = chunkMeshData;
    }

    public CameraSnapshot camera() {
        return camera;
    }

    public ChunkMeshData chunkMeshData() {
        return chunkMeshData;
    }
}
