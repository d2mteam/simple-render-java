package com.simplerender.render;

import com.simplerender.asset.MaterialData;
import com.simplerender.asset.MeshData;

public interface MeshUploader {
    MeshHandle uploadMesh(MeshData meshData);

    MaterialHandle uploadMaterial(MaterialData materialData);
}
