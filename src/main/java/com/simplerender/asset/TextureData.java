package com.simplerender.asset;

import java.util.Arrays;

public final class TextureData {
    private final int width;
    private final int height;
    private final byte[] rgba;

    public TextureData(int width, int height, byte[] rgba) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Texture dimensions must be positive");
        }
        if (rgba.length != width * height * 4) {
            throw new IllegalArgumentException("RGBA data size mismatch");
        }
        this.width = width;
        this.height = height;
        this.rgba = Arrays.copyOf(rgba, rgba.length);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public byte[] rgba() {
        return Arrays.copyOf(rgba, rgba.length);
    }
}
