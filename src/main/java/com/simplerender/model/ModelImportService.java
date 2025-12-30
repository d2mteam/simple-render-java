package com.simplerender.model;

import com.simplerender.app.EngineConfig;
import com.simplerender.world.ChunkMeshData;
import com.simplerender.world.ChunkMeshDataFactory;
import java.nio.file.Path;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ModelImportService {
    private static final Logger logger = LoggerFactory.getLogger(ModelImportService.class);

    public ChunkMeshData[] loadFromConfig(EngineConfig config) {
        String modelPath = config.modelPath();
        if (modelPath == null || modelPath.isBlank()) {
            return ChunkMeshDataFactory.randomChunks(config.chunkCount(), config.randomSeed());
        }
        ChunkMeshData imported = importModel(Path.of(modelPath.trim()));
        if (imported == null) {
            logger.warn("Falling back to random chunks because model import failed");
            return ChunkMeshDataFactory.randomChunks(config.chunkCount(), config.randomSeed());
        }
        return new ChunkMeshData[] {imported};
    }

    private ChunkMeshData importModel(Path modelPath) {
        String fileName = modelPath.getFileName() == null ? "" : modelPath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        String extension = dotIndex >= 0 ? fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT) : "";
        if ("obj".equals(extension)) {
            return ObjModelLoader.load(modelPath);
        }
        logger.error("No importer found for extension {}", extension);
        return null;
    }
}
