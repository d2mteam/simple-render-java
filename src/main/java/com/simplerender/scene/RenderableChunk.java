package com.simplerender.scene;

import com.simplerender.world.ChunkMeshData;

public final class RenderableChunk {
    private ChunkMeshData meshData;

    public RenderableChunk(ChunkMeshData meshData) {
        this.meshData = meshData;
    }

    public void updateMesh(ChunkMeshData meshData) {
        this.meshData = meshData;
    }

    public ChunkMeshData snapshot() {
        return meshData;
    }
}
