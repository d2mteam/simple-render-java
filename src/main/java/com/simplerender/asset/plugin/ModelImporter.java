package com.simplerender.asset.plugin;

import com.simplerender.asset.MaterialData;
import com.simplerender.asset.MeshData;
import java.util.List;
import org.pf4j.ExtensionPoint;

import java.nio.file.Path;

public interface ModelImporter extends ExtensionPoint {
    String[] supportedExtensions();

    ImportedModel importModel(Path path);

    record ImportedModel(List<ImportedPrimitive> primitives) {
    }

    record ImportedPrimitive(MeshData meshData, MaterialData materialData) {
    }
}
