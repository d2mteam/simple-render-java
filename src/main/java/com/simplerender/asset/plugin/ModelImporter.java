package com.simplerender.asset.plugin;

import com.simplerender.asset.MaterialData;
import com.simplerender.asset.MeshData;
import java.util.List;
import org.pf4j.ExtensionPoint;

import java.nio.file.Path;

/**
 * Plugin extension point for importing 3D model formats.
 *
 * <p>Implementations are discovered via PF4J and produce meshes, materials,
 * and per-primitive transforms that can be loaded into the scene.
 */
public interface ModelImporter extends ExtensionPoint {
    String[] supportedExtensions();

    ImportedModel importModel(Path path);

    /**
     * Bundle of imported primitives that make up a model file.
     */
    record ImportedModel(List<ImportedPrimitive> primitives) {
    }

    /**
     * Single renderable primitive with mesh, material, and local transform.
     */
    record ImportedPrimitive(MeshData meshData, MaterialData materialData, float[] transform) {
    }
}
