package com.simplerender.render;

import com.simplerender.asset.MaterialData;
import com.simplerender.asset.MeshData;
import com.simplerender.asset.TextureData;

public interface MeshUploader {
    MeshHandle uploadMesh(MeshData meshData);

    MaterialHandle uploadMaterial(MaterialData materialData);

    TextureHandle uploadTexture(TextureData textureData);
}
