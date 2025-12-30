package com.simplerender.model;

import com.simplerender.world.ChunkMeshData;
import java.nio.file.Path;

public interface ModelImporter {
    boolean supports(String extension);

    ChunkMeshData load(Path modelPath);
}
