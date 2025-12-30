package com.simplerender.model;

import com.simplerender.world.ChunkMeshData;
import java.nio.file.Path;
import java.util.Locale;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ModelImportService {
    private static final Logger logger = LoggerFactory.getLogger(ModelImportService.class);

    public ChunkMeshData importModel(Path modelPath) {
        String fileName = modelPath.getFileName() == null ? "" : modelPath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        String extension = dotIndex >= 0 ? fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT) : "";
        for (ModelImporter importer : ServiceLoader.load(ModelImporter.class)) {
            if (importer.supports(extension)) {
                return importer.load(modelPath);
            }
        }
        logger.error("No importer found for extension {}", extension);
        return null;
    }
}
