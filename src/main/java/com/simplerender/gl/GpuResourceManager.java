package com.simplerender.gl;

import com.simplerender.asset.MaterialData;
import com.simplerender.asset.MeshData;
import com.simplerender.asset.SamplerData;
import com.simplerender.asset.TextureData;
import com.simplerender.asset.TextureSlot;
import com.simplerender.render.MaterialHandle;
import com.simplerender.render.MeshHandle;
import com.simplerender.render.SamplerHandle;
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
    private final AtomicInteger samplerId = new AtomicInteger();
    private final Map<MeshHandle, GPUMesh> meshes = new HashMap<>();
    private final Map<MaterialHandle, GpuMaterial> materials = new HashMap<>();
    private final Map<TextureHandle, GpuTexture> textures = new HashMap<>();
    private final Map<SamplerHandle, GpuSampler> samplers = new HashMap<>();
    private TextureHandle defaultBaseColorTexture;
    private TextureHandle defaultNormalTexture;
    private TextureHandle defaultMetallicRoughnessTexture;
    private TextureHandle defaultAoTexture;
    private TextureHandle defaultEmissiveTexture;
    private SamplerHandle defaultSampler;

    public MeshHandle uploadMesh(MeshData meshData) {
        MeshHandle handle = new MeshHandle(meshId.incrementAndGet());
        GPUMesh mesh = new GPUMesh();
        mesh.upload(meshData);
        meshes.put(handle, mesh);
        logger.info("Uploaded mesh handle {}", handle.hashCode());
        return handle;
    }

    public MaterialHandle uploadMaterial(MaterialData materialData) {
        TextureHandle baseColorHandle = resolveTexture(materialData.baseColorTexture(), defaultBaseColorTexture);
        SamplerHandle baseColorSampler = resolveSampler(materialData.baseColorTexture(), defaultSampler);
        TextureHandle normalHandle = resolveTexture(materialData.normalTexture(), defaultNormalTexture);
        SamplerHandle normalSampler = resolveSampler(materialData.normalTexture(), defaultSampler);
        TextureHandle metallicRoughnessHandle = resolveTexture(
            materialData.metallicRoughnessTexture(),
            defaultMetallicRoughnessTexture
        );
        SamplerHandle metallicRoughnessSampler = resolveSampler(materialData.metallicRoughnessTexture(), defaultSampler);
        TextureHandle aoHandle = resolveTexture(materialData.aoTexture(), defaultAoTexture);
        SamplerHandle aoSampler = resolveSampler(materialData.aoTexture(), defaultSampler);
        TextureHandle emissiveHandle = resolveTexture(materialData.emissiveTexture(), defaultEmissiveTexture);
        SamplerHandle emissiveSampler = resolveSampler(materialData.emissiveTexture(), defaultSampler);
        MaterialHandle handle = new MaterialHandle(materialId.incrementAndGet());
        materials.put(handle, new GpuMaterial(
            materialData.baseColor(),
            baseColorHandle,
            baseColorSampler,
            normalHandle,
            normalSampler,
            metallicRoughnessHandle,
            metallicRoughnessSampler,
            aoHandle,
            aoSampler,
            emissiveHandle,
            emissiveSampler
        ));
        logger.info("Uploaded material handle {}", handle.hashCode());
        return handle;
    }

    public TextureHandle uploadTexture(TextureData textureData) {
        TextureHandle handle = new TextureHandle(textureId.incrementAndGet());
        textures.put(handle, new GpuTexture(textureData));
        logger.info("Uploaded texture handle {}", handle.hashCode());
        return handle;
    }

    public SamplerHandle uploadSampler(SamplerData samplerData) {
        SamplerHandle handle = new SamplerHandle(samplerId.incrementAndGet());
        samplers.put(handle, new GpuSampler(samplerData));
        logger.info("Uploaded sampler handle {}", handle.hashCode());
        return handle;
    }

    public void initDefaultTextures(
        TextureData baseColorTexture,
        TextureData normalTexture,
        TextureData metallicRoughnessTexture,
        TextureData aoTexture,
        TextureData emissiveTexture
    ) {
        if (defaultBaseColorTexture == null) {
            defaultBaseColorTexture = uploadTexture(baseColorTexture);
        }
        if (defaultNormalTexture == null) {
            defaultNormalTexture = uploadTexture(normalTexture);
        }
        if (defaultMetallicRoughnessTexture == null) {
            defaultMetallicRoughnessTexture = uploadTexture(metallicRoughnessTexture);
        }
        if (defaultAoTexture == null) {
            defaultAoTexture = uploadTexture(aoTexture);
        }
        if (defaultEmissiveTexture == null) {
            defaultEmissiveTexture = uploadTexture(emissiveTexture);
        }
    }

    public void initDefaultSampler(SamplerData samplerData) {
        if (defaultSampler == null) {
            defaultSampler = uploadSampler(samplerData);
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

    public GpuSampler sampler(SamplerHandle handle) {
        return samplers.get(handle);
    }

    public GpuTexture defaultTexture() {
        return textures.get(defaultBaseColorTexture);
    }

    public GpuSampler defaultSampler() {
        return samplers.get(defaultSampler);
    }

    private TextureHandle resolveTexture(java.util.Optional<TextureSlot> slot, TextureHandle fallback) {
        return slot.map(TextureSlot::textureData).map(this::uploadTexture).orElse(fallback);
    }

    private SamplerHandle resolveSampler(java.util.Optional<TextureSlot> slot, SamplerHandle fallback) {
        return slot.flatMap(TextureSlot::samplerData).map(this::uploadSampler).orElse(fallback);
    }

    record GpuMaterial(
        float[] baseColor,
        TextureHandle baseColorTexture,
        SamplerHandle baseColorSampler,
        TextureHandle normalTexture,
        SamplerHandle normalSampler,
        TextureHandle metallicRoughnessTexture,
        SamplerHandle metallicRoughnessSampler,
        TextureHandle aoTexture,
        SamplerHandle aoSampler,
        TextureHandle emissiveTexture,
        SamplerHandle emissiveSampler
    ) {
    }
}
