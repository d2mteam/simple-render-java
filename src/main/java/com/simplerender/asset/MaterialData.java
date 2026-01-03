package com.simplerender.asset;

import java.util.Arrays;
import java.util.Optional;

public final class MaterialData {
    private final float[] baseColor;
    private final TextureSlot baseColorTexture;
    private final TextureSlot normalTexture;
    private final TextureSlot metallicRoughnessTexture;
    private final TextureSlot aoTexture;
    private final TextureSlot emissiveTexture;

    public MaterialData(float[] baseColor, TextureData textureData) {
        this(baseColor, textureData, null, null, null, null);
    }

    public MaterialData(
        float[] baseColor,
        TextureData baseColorTexture,
        TextureData normalTexture,
        TextureData metallicRoughnessTexture,
        TextureData aoTexture,
        TextureData emissiveTexture
    ) {
        this(
            baseColor,
            toSlot(baseColorTexture, TextureColorSpace.SRGB),
            toSlot(normalTexture, TextureColorSpace.LINEAR),
            toSlot(metallicRoughnessTexture, TextureColorSpace.LINEAR),
            toSlot(aoTexture, TextureColorSpace.LINEAR),
            toSlot(emissiveTexture, TextureColorSpace.SRGB)
        );
    }

    public MaterialData(
        float[] baseColor,
        TextureSlot baseColorTexture,
        TextureSlot normalTexture,
        TextureSlot metallicRoughnessTexture,
        TextureSlot aoTexture,
        TextureSlot emissiveTexture
    ) {
        if (baseColor.length != 3) {
            throw new IllegalArgumentException("Base color must have 3 components");
        }
        this.baseColor = Arrays.copyOf(baseColor, baseColor.length);
        this.baseColorTexture = baseColorTexture;
        this.normalTexture = normalTexture;
        this.metallicRoughnessTexture = metallicRoughnessTexture;
        this.aoTexture = aoTexture;
        this.emissiveTexture = emissiveTexture;
    }

    public float[] baseColor() {
        return Arrays.copyOf(baseColor, baseColor.length);
    }

    public Optional<TextureData> textureData() {
        return Optional.ofNullable(baseColorTexture).map(TextureSlot::textureData);
    }

    public Optional<TextureSlot> baseColorTexture() {
        return Optional.ofNullable(baseColorTexture);
    }

    public Optional<TextureSlot> normalTexture() {
        return Optional.ofNullable(normalTexture);
    }

    public Optional<TextureSlot> metallicRoughnessTexture() {
        return Optional.ofNullable(metallicRoughnessTexture);
    }

    public Optional<TextureSlot> aoTexture() {
        return Optional.ofNullable(aoTexture);
    }

    public Optional<TextureSlot> emissiveTexture() {
        return Optional.ofNullable(emissiveTexture);
    }

    private static TextureSlot toSlot(TextureData textureData, TextureColorSpace colorSpace) {
        return textureData == null ? null : new TextureSlot(textureData.withColorSpace(colorSpace), null);
    }
}
