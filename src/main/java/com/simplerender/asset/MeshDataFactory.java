package com.simplerender.asset;

import com.simplerender.world.ChunkMeshData;

public final class MeshDataFactory {
    private MeshDataFactory() {
    }

    public static MeshData fromChunkMeshData(ChunkMeshData chunkMeshData) {
        return new MeshData(
            chunkMeshData.positions(),
            chunkMeshData.normals(),
            chunkMeshData.indices()
        );
    }
}
