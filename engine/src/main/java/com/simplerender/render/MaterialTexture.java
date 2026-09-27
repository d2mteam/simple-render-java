package com.simplerender.render;

import com.simplerender.asset.MaterialData;
import com.simplerender.asset.TextureSlot;
import java.util.Optional;

/**
 * The texture slots of a material, with the shader uniforms each one feeds.
 * The texture unit a slot is bound to is its ordinal (base colour = unit 0, ...).
 */
enum MaterialTexture {
    BASE_COLOR("uBaseColorTex", "uBaseColorTexCoord"),
    NORMAL("uNormalTex", "uNormalTexCoord"),
    METALLIC_ROUGHNESS("uMetallicRoughnessTex", "uMetallicRoughnessTexCoord"),
    OCCLUSION("uAoTex", "uAoTexCoord"),
    EMISSIVE("uEmissiveTex", "uEmissiveTexCoord");

    /** {@code sampler2D} uniform that reads this slot. */
    final String samplerUniform;
    /** {@code int} uniform choosing UV set 0 or 1 for this slot. */
    final String texCoordUniform;

    MaterialTexture(String samplerUniform, String texCoordUniform) {
        this.samplerUniform = samplerUniform;
        this.texCoordUniform = texCoordUniform;
    }

    int unit() {
        return ordinal();
    }

    Optional<TextureSlot> in(MaterialData material) {
        return switch (this) {
            case BASE_COLOR -> material.baseColorTexture();
            case NORMAL -> material.normalTexture();
            case METALLIC_ROUGHNESS -> material.metallicRoughnessTexture();
            case OCCLUSION -> material.aoTexture();
            case EMISSIVE -> material.emissiveTexture();
        };
    }
}
