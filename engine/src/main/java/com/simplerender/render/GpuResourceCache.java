package com.simplerender.render;

import com.simplerender.asset.MaterialData;
import com.simplerender.asset.MeshData;
import com.simplerender.asset.SamplerData;
import com.simplerender.asset.TextureData;
import com.simplerender.asset.TextureDataFactory;
import com.simplerender.asset.TextureSlot;
import com.simplerender.render.gl.GpuMesh;
import com.simplerender.render.gl.GpuSampler;
import com.simplerender.render.gl.GpuTexture;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Uploads CPU assets to the GPU on first use and remembers the result.
 *
 * <p>Meshes, materials and textures are keyed by object identity, so the same
 * {@link MeshData} drawn many times is uploaded once. Samplers are keyed by value, so all
 * materials with the same filtering share one sampler object. Resources stay alive until
 * the renderer is closed.
 */
final class GpuResourceCache implements AutoCloseable {

    /** One texture slot, ready to bind. */
    record TextureBinding(GpuTexture texture, GpuSampler sampler, int texCoord) {
    }

    /** A material with all its texture slots resolved (missing slots use fallback textures). */
    record GpuMaterial(MaterialData data, List<TextureBinding> textures) {
        TextureBinding texture(MaterialTexture slot) {
            return textures.get(slot.ordinal());
        }
    }

    private final Map<MeshData, GpuMesh> meshes = new IdentityHashMap<>();
    private final Map<MaterialData, GpuMaterial> materials = new IdentityHashMap<>();
    private final Map<TextureData, GpuTexture> textures = new IdentityHashMap<>();
    private final Map<SamplerData, GpuSampler> samplers = new HashMap<>();
    private final Map<MaterialTexture, GpuTexture> fallbackTextures = new EnumMap<>(MaterialTexture.class);
    private final GpuSampler defaultSampler;

    GpuResourceCache() {
        // Neutral values: a checkerboard base colour so missing UVs are visible, a flat normal,
        // fully rough non-metal, no occlusion, no emission.
        fallbackTextures.put(MaterialTexture.BASE_COLOR, texture(TextureDataFactory.checkerboard(512, 64)));
        fallbackTextures.put(MaterialTexture.NORMAL, texture(TextureDataFactory.solidColor(128, 128, 255, 255)));
        fallbackTextures.put(MaterialTexture.METALLIC_ROUGHNESS, texture(TextureDataFactory.solidColor(0, 255, 0, 255)));
        fallbackTextures.put(MaterialTexture.OCCLUSION, texture(TextureDataFactory.solidColor(255, 255, 255, 255)));
        fallbackTextures.put(MaterialTexture.EMISSIVE, texture(TextureDataFactory.solidColor(0, 0, 0, 255)));
        defaultSampler = sampler(SamplerData.defaults());
    }

    GpuMesh mesh(MeshData mesh) {
        return meshes.computeIfAbsent(mesh, GpuMesh::new);
    }

    GpuMaterial material(MaterialData material) {
        return materials.computeIfAbsent(material, this::upload);
    }

    private GpuMaterial upload(MaterialData material) {
        List<TextureBinding> bindings = new ArrayList<>();
        for (MaterialTexture slot : MaterialTexture.values()) {
            Optional<TextureSlot> textureSlot = slot.in(material);
            GpuTexture texture = textureSlot
                    .map(s -> texture(s.textureData()))
                    .orElse(fallbackTextures.get(slot));
            GpuSampler sampler = textureSlot
                    .flatMap(TextureSlot::samplerData)
                    .map(this::sampler)
                    .orElse(defaultSampler);
            int texCoord = textureSlot.map(TextureSlot::texCoord).orElse(0);
            bindings.add(new TextureBinding(texture, sampler, texCoord));
        }
        return new GpuMaterial(material, List.copyOf(bindings));
    }

    private GpuTexture texture(TextureData texture) {
        return textures.computeIfAbsent(texture, GpuTexture::new);
    }

    private GpuSampler sampler(SamplerData sampler) {
        return samplers.computeIfAbsent(sampler, GpuSampler::new);
    }

    @Override
    public void close() {
        meshes.values().forEach(GpuMesh::close);
        textures.values().forEach(GpuTexture::close);
        samplers.values().forEach(GpuSampler::close);
        meshes.clear();
        materials.clear();
        textures.clear();
        samplers.clear();
    }
}
