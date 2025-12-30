package com.simplerender.asset.plugin;

import com.simplerender.asset.MaterialData;
import com.simplerender.asset.MeshData;
import org.pf4j.ExtensionPoint;

import java.nio.file.Path;

public interface ModelImporter extends ExtensionPoint {
    ImportedModel importModel(Path path);

    record ImportedModel(MeshData meshData, MaterialData materialData) {
    }
}
