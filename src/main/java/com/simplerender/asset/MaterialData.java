package com.simplerender.asset;

import java.util.Arrays;

public final class MaterialData {
    private final float[] baseColor;

    public MaterialData(float[] baseColor) {
        if (baseColor.length != 3) {
            throw new IllegalArgumentException("Base color must have 3 components");
        }
        this.baseColor = Arrays.copyOf(baseColor, baseColor.length);
    }

    public float[] baseColor() {
        return Arrays.copyOf(baseColor, baseColor.length);
    }
}
