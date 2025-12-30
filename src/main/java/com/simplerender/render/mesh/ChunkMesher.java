package com.simplerender.render.mesh;

import com.simplerender.world.ChunkBlockView;

public final class ChunkMesher {
    public void buildMesh(ChunkBlockView blocks, MeshData meshData) {
        int solidBlocks = 0;
        for (int i = 0; i < blocks.blockCount(); i++) {
            if (blocks.blockAt(i) != 0) {
                solidBlocks++;
            }
        }
        int vertexCount = solidBlocks * 4;
        int indexCount = solidBlocks * 6;
        meshData.setCounts(vertexCount, indexCount);
    }
}
