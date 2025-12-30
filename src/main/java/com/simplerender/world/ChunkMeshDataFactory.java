package com.simplerender.world;

import java.util.Random;

public final class ChunkMeshDataFactory {
    private ChunkMeshDataFactory() {
    }

    public static ChunkMeshData[] randomChunks(int count, long seed) {
        Random random = new Random(seed);
        ChunkMeshData[] chunks = new ChunkMeshData[count];
        for (int i = 0; i < count; i++) {
            float baseX = random.nextInt(8) * 2.0f;
            float baseY = random.nextInt(4) * 1.5f;
            float baseZ = random.nextInt(8) * 2.0f;
            chunks[i] = createQuadChunk(baseX, baseY, baseZ);
        }
        return chunks;
    }

    private static ChunkMeshData createQuadChunk(float baseX, float baseY, float baseZ) {
        float[] positions = new float[] {
            baseX, baseY, baseZ,
            baseX + 1.0f, baseY, baseZ,
            baseX + 1.0f, baseY + 1.0f, baseZ,
            baseX, baseY + 1.0f, baseZ
        };
        float[] normals = new float[] {
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f
        };
        int[] indices = new int[] {0, 1, 2, 2, 3, 0};
        return new ChunkMeshData(4, positions, normals, indices);
    }
}
