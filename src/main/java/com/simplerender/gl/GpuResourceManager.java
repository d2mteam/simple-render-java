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

/**
 * Registry for CPU-to-GPU resource uploads and handle lookups.
 *
 * <p>Creates stable handles for meshes, materials, textures, and samplers and
 * stores their GPU counterparts for retrieval during rendering.
 */
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

    public MeshHandle createMeshHandle() {
        return new MeshHandle(meshId.incrementAndGet());
    }

    public MaterialHandle createMaterialHandle() {
        return new MaterialHandle(materialId.incrementAndGet());
    }

    public TextureHandle createTextureHandle() {
        return new TextureHandle(textureId.incrementAndGet());
    }

    public SamplerHandle createSamplerHandle() {
        return new SamplerHandle(samplerId.incrementAndGet());
    }

    public MeshHandle uploadMesh(MeshData meshData) {
        MeshHandle handle = createMeshHandle();
        uploadMesh(handle, meshData, MeshUploadMode.STATIC_ONE_SHOT);
        return handle;
    }

    public void uploadMesh(MeshHandle handle, MeshData meshData, MeshUploadMode uploadMode) {
        GPUMesh mesh = new GPUMesh();
        mesh.upload(meshData, uploadMode);
        meshes.put(handle, mesh);
        logger.info("Uploaded mesh handle {}", handle.hashCode());
    }

    public MaterialHandle uploadMaterial(MaterialData materialData) {
        MaterialHandle handle = createMaterialHandle();
        uploadMaterial(handle, materialData, this::uploadTexture, this::uploadSampler);
        return handle;
    }

    public void uploadMaterial(
            MaterialHandle handle,
            MaterialData materialData,
            TextureUploadDelegate textureUpload,
            SamplerUploadDelegate samplerUpload) {
        TextureHandle baseColorHandle = resolveTexture(materialData.baseColorTexture(), defaultBaseColorTexture,
                textureUpload);
        SamplerHandle baseColorSampler = resolveSampler(materialData.baseColorTexture(), defaultSampler, samplerUpload);
        int baseColorTexCoord = resolveTexCoord(materialData.baseColorTexture());
        TextureHandle normalHandle = resolveTexture(materialData.normalTexture(), defaultNormalTexture, textureUpload);
        SamplerHandle normalSampler = resolveSampler(materialData.normalTexture(), defaultSampler, samplerUpload);
        int normalTexCoord = resolveTexCoord(materialData.normalTexture());
        TextureHandle metallicRoughnessHandle = resolveTexture(
                materialData.metallicRoughnessTexture(),
                defaultMetallicRoughnessTexture,
                textureUpload);
        SamplerHandle metallicRoughnessSampler = resolveSampler(materialData.metallicRoughnessTexture(), defaultSampler,
                samplerUpload);
        int metallicRoughnessTexCoord = resolveTexCoord(materialData.metallicRoughnessTexture());
        TextureHandle aoHandle = resolveTexture(materialData.aoTexture(), defaultAoTexture, textureUpload);
        SamplerHandle aoSampler = resolveSampler(materialData.aoTexture(), defaultSampler, samplerUpload);
        int aoTexCoord = resolveTexCoord(materialData.aoTexture());
        TextureHandle emissiveHandle = resolveTexture(materialData.emissiveTexture(), defaultEmissiveTexture,
                textureUpload);
        SamplerHandle emissiveSampler = resolveSampler(materialData.emissiveTexture(), defaultSampler, samplerUpload);
        int emissiveTexCoord = resolveTexCoord(materialData.emissiveTexture());
        materials.put(handle, new GpuMaterial(
                materialData.baseColor(),
                baseColorHandle,
                baseColorSampler,
                baseColorTexCoord,
                normalHandle,
                normalSampler,
                normalTexCoord,
                metallicRoughnessHandle,
                metallicRoughnessSampler,
                metallicRoughnessTexCoord,
                aoHandle,
                aoSampler,
                aoTexCoord,
                emissiveHandle,
                emissiveSampler,
                emissiveTexCoord,
                materialData.alphaMode(),
                materialData.alphaCutoff()));
        logger.info("Uploaded material handle {}", handle.hashCode());
    }

    public TextureHandle uploadTexture(TextureData textureData) {
        TextureHandle handle = createTextureHandle();
        registerTexture(handle, new GpuTexture(textureData));
        return handle;
    }

    public SamplerHandle uploadSampler(SamplerData samplerData) {
        SamplerHandle handle = createSamplerHandle();
        registerSampler(handle, new GpuSampler(samplerData));
        return handle;
    }

    public void registerTexture(TextureHandle handle, GpuTexture texture) {
        textures.put(handle, texture);
        logger.info("Uploaded texture handle {}", handle.hashCode());
    }

    public void registerSampler(SamplerHandle handle, GpuSampler sampler) {
        samplers.put(handle, sampler);
        logger.info("Uploaded sampler handle {}", handle.hashCode());
    }

    public void initDefaultTextures(
            TextureData baseColorTexture,
            TextureData normalTexture,
            TextureData metallicRoughnessTexture,
            TextureData aoTexture,
            TextureData emissiveTexture) {
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

    public GpuTexture defaultBaseColorTexture() {
        return textures.get(defaultBaseColorTexture);
    }

    public GpuTexture defaultNormalTexture() {
        return textures.get(defaultNormalTexture);
    }

    public GpuTexture defaultMetallicRoughnessTexture() {
        return textures.get(defaultMetallicRoughnessTexture);
    }

    public GpuTexture defaultAoTexture() {
        return textures.get(defaultAoTexture);
    }

    public GpuTexture defaultEmissiveTexture() {
        return textures.get(defaultEmissiveTexture);
    }

    public GpuSampler defaultSampler() {
        return samplers.get(defaultSampler);
    }

    private TextureHandle resolveTexture(
            java.util.Optional<TextureSlot> slot,
            TextureHandle fallback,
            TextureUploadDelegate textureUpload) {
        return slot.map(TextureSlot::textureData).map(textureUpload::upload).orElse(fallback);
    }

    private SamplerHandle resolveSampler(
            java.util.Optional<TextureSlot> slot,
            SamplerHandle fallback,
            SamplerUploadDelegate samplerUpload) {
        return slot.flatMap(TextureSlot::samplerData).map(samplerUpload::upload).orElse(fallback);
    }

    private int resolveTexCoord(java.util.Optional<TextureSlot> slot) {
        return slot.map(TextureSlot::texCoord).orElse(0);
    }

    /**
     * Delegate used for deferred texture uploads in material creation.
     */
    @FunctionalInterface
    interface TextureUploadDelegate {
        TextureHandle upload(TextureData textureData);
    }

    /**
     * Delegate used for deferred sampler uploads in material creation.
     */
    @FunctionalInterface
    interface SamplerUploadDelegate {
        SamplerHandle upload(SamplerData samplerData);
    }

    /**
     * GPU-ready material binding information.
     */
    record GpuMaterial(
            float[] baseColor,
            TextureHandle baseColorTexture,
            SamplerHandle baseColorSampler,
            int baseColorTexCoord,
            TextureHandle normalTexture,
            SamplerHandle normalSampler,
            int normalTexCoord,
            TextureHandle metallicRoughnessTexture,
            SamplerHandle metallicRoughnessSampler,
            int metallicRoughnessTexCoord,
            TextureHandle aoTexture,
            SamplerHandle aoSampler,
            int aoTexCoord,
            TextureHandle emissiveTexture,
            SamplerHandle emissiveSampler,
            int emissiveTexCoord,
            String alphaMode,
            float alphaCutoff) {
    }
}
