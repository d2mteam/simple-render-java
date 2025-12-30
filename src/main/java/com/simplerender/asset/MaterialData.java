package com.simplerender.asset;

import java.util.Arrays;
import java.util.Optional;

public final class MaterialData {
    private final float[] baseColor;
    private final TextureData baseColorTexture;
    private final TextureData metallicRoughnessTexture;
    private final TextureData normalTexture;
    private final TextureData occlusionTexture;
    private final TextureData emissiveTexture;

    public MaterialData(float[] baseColor, TextureData baseColorTexture) {
        this(baseColor, baseColorTexture, null, null, null, null);
    }

    public MaterialData(
        float[] baseColor,
        TextureData baseColorTexture,
        TextureData metallicRoughnessTexture,
        TextureData normalTexture,
        TextureData occlusionTexture,
        TextureData emissiveTexture
    ) {
        if (baseColor.length != 3) {
            throw new IllegalArgumentException("Base color must have 3 components");
        }
        this.baseColor = Arrays.copyOf(baseColor, baseColor.length);
        this.baseColorTexture = baseColorTexture;
        this.metallicRoughnessTexture = metallicRoughnessTexture;
        this.normalTexture = normalTexture;
        this.occlusionTexture = occlusionTexture;
        this.emissiveTexture = emissiveTexture;
    }

    public float[] baseColor() {
        return Arrays.copyOf(baseColor, baseColor.length);
    }

    public Optional<TextureData> textureData() {
        return Optional.ofNullable(baseColorTexture);
    }

    public Optional<TextureData> baseColorTexture() {
        return Optional.ofNullable(baseColorTexture);
    }

    public Optional<TextureData> metallicRoughnessTexture() {
        return Optional.ofNullable(metallicRoughnessTexture);
    }

    public Optional<TextureData> normalTexture() {
        return Optional.ofNullable(normalTexture);
    }

    public Optional<TextureData> occlusionTexture() {
        return Optional.ofNullable(occlusionTexture);
    }

    public Optional<TextureData> emissiveTexture() {
        return Optional.ofNullable(emissiveTexture);
    }
}
