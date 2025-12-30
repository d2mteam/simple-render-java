package com.simplerender.asset;

import java.util.Arrays;
import java.util.Optional;

public final class MaterialData {
    private final float[] baseColor;
    private final TextureData textureData;

    public MaterialData(float[] baseColor, TextureData textureData) {
        if (baseColor.length != 3) {
            throw new IllegalArgumentException("Base color must have 3 components");
        }
        this.baseColor = Arrays.copyOf(baseColor, baseColor.length);
        this.textureData = textureData;
    }

    public float[] baseColor() {
        return Arrays.copyOf(baseColor, baseColor.length);
    }

    public Optional<TextureData> textureData() {
        return Optional.ofNullable(textureData);
    }
}
