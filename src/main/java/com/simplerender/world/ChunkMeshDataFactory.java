package com.simplerender.world;

import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ChunkMeshDataFactory {
    private static final Logger logger = LoggerFactory.getLogger(ChunkMeshDataFactory.class);

    private ChunkMeshDataFactory() {
    }

    public static ChunkMeshData[] randomChunks(int count, long seed) {
        if (count <= 0) {
            logger.error("Chunk count must be positive, got {}", count);
            return new ChunkMeshData[0];
        }
        Random random = new Random(seed);
        ChunkMeshData[] chunks = new ChunkMeshData[count];
        for (int i = 0; i < count; i++) {
            float baseX = random.nextInt(8) * 2.0f;
            float baseY = random.nextInt(4) * 1.5f;
            float baseZ = random.nextInt(8) * 2.0f;
            chunks[i] = createQuadChunk(baseX, baseY, baseZ);
        }
        logger.info("Generated {} random chunk mesh snapshots with seed {}", count, seed);
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
