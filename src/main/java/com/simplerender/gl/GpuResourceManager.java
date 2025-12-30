package com.simplerender.gl;

import com.simplerender.asset.MaterialData;
import com.simplerender.asset.MeshData;
import com.simplerender.render.MaterialHandle;
import com.simplerender.render.MeshHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

final class GpuResourceManager {
    private static final Logger logger = LoggerFactory.getLogger(GpuResourceManager.class);

    private final AtomicInteger meshId = new AtomicInteger();
    private final AtomicInteger materialId = new AtomicInteger();
    private final Map<MeshHandle, GPUMesh> meshes = new HashMap<>();
    private final Map<MaterialHandle, GpuMaterial> materials = new HashMap<>();

    public MeshHandle uploadMesh(MeshData meshData) {
        MeshHandle handle = new MeshHandle(meshId.incrementAndGet());
        GPUMesh mesh = new GPUMesh();
        mesh.upload(meshData);
        meshes.put(handle, mesh);
        logger.info("Uploaded mesh handle {}", handle.hashCode());
        return handle;
    }

    public MaterialHandle uploadMaterial(MaterialData materialData) {
        MaterialHandle handle = new MaterialHandle(materialId.incrementAndGet());
        materials.put(handle, new GpuMaterial(materialData.baseColor()));
        logger.info("Uploaded material handle {}", handle.hashCode());
        return handle;
    }

    public GPUMesh mesh(MeshHandle handle) {
        return meshes.get(handle);
    }

    public GpuMaterial material(MaterialHandle handle) {
        return materials.get(handle);
    }

    record GpuMaterial(float[] baseColor) {
    }
}
