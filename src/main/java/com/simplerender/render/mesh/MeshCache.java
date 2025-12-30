package com.simplerender.render.mesh;

import com.simplerender.world.ChunkMeshData;

public final class MeshCache {
    private ChunkMeshData lastSnapshot;

    public boolean needsUpload(ChunkMeshData snapshot) {
        return snapshot != lastSnapshot;
    }

    public void updateSnapshot(ChunkMeshData snapshot) {
        this.lastSnapshot = snapshot;
    }
}
