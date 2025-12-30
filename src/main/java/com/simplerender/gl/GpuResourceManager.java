package com.simplerender.gl;

import com.simplerender.asset.MaterialData;
import com.simplerender.asset.MeshData;
import com.simplerender.asset.TextureData;
import com.simplerender.render.MaterialHandle;
import com.simplerender.render.MeshHandle;
import com.simplerender.render.TextureHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

final class GpuResourceManager {
    private static final Logger logger = LoggerFactory.getLogger(GpuResourceManager.class);

    private final AtomicInteger meshId = new AtomicInteger();
    private final AtomicInteger materialId = new AtomicInteger();
    private final AtomicInteger textureId = new AtomicInteger();
    private final Map<MeshHandle, GPUMesh> meshes = new HashMap<>();
    private final Map<MaterialHandle, GpuMaterial> materials = new HashMap<>();
    private final Map<TextureHandle, GpuTexture> textures = new HashMap<>();
    private TextureHandle defaultTexture;

    public MeshHandle uploadMesh(MeshData meshData) {
        MeshHandle handle = new MeshHandle(meshId.incrementAndGet());
        GPUMesh mesh = new GPUMesh();
        mesh.upload(meshData);
        meshes.put(handle, mesh);
        logger.info("Uploaded mesh handle {}", handle.hashCode());
        return handle;
    }

    public MaterialHandle uploadMaterial(MaterialData materialData) {
        TextureHandle textureHandle = materialData.textureData()
            .map(this::uploadTexture)
            .orElseGet(() -> defaultTexture);
        MaterialHandle handle = new MaterialHandle(materialId.incrementAndGet());
        materials.put(handle, new GpuMaterial(materialData.baseColor(), textureHandle));
        logger.info("Uploaded material handle {}", handle.hashCode());
        return handle;
    }

    public TextureHandle uploadTexture(TextureData textureData) {
        TextureHandle handle = new TextureHandle(textureId.incrementAndGet());
        textures.put(handle, new GpuTexture(textureData));
        logger.info("Uploaded texture handle {}", handle.hashCode());
        return handle;
    }

    public void initDefaultTexture(TextureData textureData) {
        if (defaultTexture == null) {
            defaultTexture = uploadTexture(textureData);
        }
    }

    public GPUMesh mesh(MeshHandle handle) {
        return meshes.get(handle);
    }

    public GpuMaterial material(MaterialHandle handle) {
        return materials.get(handle);
    }

    public GpuTexture texture(TextureHandle handle) {
        return textures.get(handle);
    }

    record GpuMaterial(float[] baseColor, TextureHandle textureHandle) {
        TextureHandle baseColorTexture() {
            return textureHandle;
        }
    }
}
