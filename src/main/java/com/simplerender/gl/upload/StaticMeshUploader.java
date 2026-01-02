package com.simplerender.gl.upload;

import com.simplerender.asset.MeshData;
import com.simplerender.gl.GPUMesh;
import com.simplerender.gl.MeshUploadMode;

public final class StaticMeshUploader {
    public GPUMesh upload(MeshData meshData) {
        GPUMesh mesh = new GPUMesh();
        mesh.upload(meshData, MeshUploadMode.STATIC_ONE_SHOT);
        return mesh;
    }
}
