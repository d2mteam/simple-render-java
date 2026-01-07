package com.simplerender.asset;

import java.util.Arrays;

/**
 * CPU-side texture payload containing RGBA pixels and color space metadata.
 *
 * <p>{@link TextureData} is immutable and safe to pass between threads; it is
 * later uploaded to the GPU by {@code GpuResourceManager}.
 */
public final class TextureData {
    private final int width;
    private final int height;
    private final byte[] rgba;
    private final TextureColorSpace colorSpace;

    public TextureData(int width, int height, byte[] rgba) {
        this(width, height, rgba, TextureColorSpace.LINEAR);
    }

    public TextureData(int width, int height, byte[] rgba, TextureColorSpace colorSpace) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Texture dimensions must be positive");
        }
        if (rgba.length != width * height * 4) {
            throw new IllegalArgumentException("RGBA data size mismatch");
        }
        if (colorSpace == null) {
            throw new IllegalArgumentException("Color space is required");
        }
        this.width = width;
        this.height = height;
        this.rgba = Arrays.copyOf(rgba, rgba.length);
        this.colorSpace = colorSpace;
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

    public TextureColorSpace colorSpace() {
        return colorSpace;
    }

    public TextureData withColorSpace(TextureColorSpace colorSpace) {
        if (this.colorSpace == colorSpace) {
            return this;
        }
        return new TextureData(width, height, rgba, colorSpace);
    }
}
