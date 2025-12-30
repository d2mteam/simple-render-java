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
            float baseX = random.nextInt(6) * 2.0f - 6.0f;
            float baseY = random.nextInt(3) * 1.5f;
            float baseZ = random.nextInt(6) * 2.0f - 6.0f;
            chunks[i] = createCubeChunk(baseX, baseY, baseZ, 1.0f);
        }
        logger.info("Generated {} random chunk mesh snapshots with seed {}", count, seed);
        return chunks;
    }

    private static ChunkMeshData createCubeChunk(float baseX, float baseY, float baseZ, float size) {
        float x0 = baseX;
        float x1 = baseX + size;
        float y0 = baseY;
        float y1 = baseY + size;
        float z0 = baseZ;
        float z1 = baseZ + size;

        float[] positions = new float[] {
            // Front
            x0, y0, z1,  x1, y0, z1,  x1, y1, z1,  x0, y1, z1,
            // Back
            x1, y0, z0,  x0, y0, z0,  x0, y1, z0,  x1, y1, z0,
            // Left
            x0, y0, z0,  x0, y0, z1,  x0, y1, z1,  x0, y1, z0,
            // Right
            x1, y0, z1,  x1, y0, z0,  x1, y1, z0,  x1, y1, z1,
            // Top
            x0, y1, z1,  x1, y1, z1,  x1, y1, z0,  x0, y1, z0,
            // Bottom
            x0, y0, z0,  x1, y0, z0,  x1, y0, z1,  x0, y0, z1
        };

        float[] normals = new float[] {
            // Front
            0f, 0f, 1f,  0f, 0f, 1f,  0f, 0f, 1f,  0f, 0f, 1f,
            // Back
            0f, 0f, -1f,  0f, 0f, -1f,  0f, 0f, -1f,  0f, 0f, -1f,
            // Left
            -1f, 0f, 0f,  -1f, 0f, 0f,  -1f, 0f, 0f,  -1f, 0f, 0f,
            // Right
            1f, 0f, 0f,  1f, 0f, 0f,  1f, 0f, 0f,  1f, 0f, 0f,
            // Top
            0f, 1f, 0f,  0f, 1f, 0f,  0f, 1f, 0f,  0f, 1f, 0f,
            // Bottom
            0f, -1f, 0f,  0f, -1f, 0f,  0f, -1f, 0f,  0f, -1f, 0f
        };

        int[] indices = new int[] {
            0, 1, 2, 2, 3, 0,
            4, 5, 6, 6, 7, 4,
            8, 9, 10, 10, 11, 8,
            12, 13, 14, 14, 15, 12,
            16, 17, 18, 18, 19, 16,
            20, 21, 22, 22, 23, 20
        };
        return new ChunkMeshData(24, positions, normals, indices);
    }
}
