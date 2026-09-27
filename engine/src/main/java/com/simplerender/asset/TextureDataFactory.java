package com.simplerender.asset;

/**
 * Generates small procedural textures, used as fallbacks when a material has no texture.
 */
public final class TextureDataFactory {
    private TextureDataFactory() {
    }

    /** Grey checkerboard, handy for spotting UV problems on untextured models. */
    public static TextureData checkerboard(int size, int cellSize) {
        byte[] rgba = new byte[size * size * 4];
        int index = 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                boolean even = ((x / cellSize) + (y / cellSize)) % 2 == 0;
                byte value = (byte) (even ? 220 : 60);
                rgba[index++] = value;
                rgba[index++] = value;
                rgba[index++] = value;
                rgba[index++] = (byte) 255;
            }
        }
        return new TextureData(size, size, rgba, TextureColorSpace.LINEAR);
    }

    /** 1x1 texture of a single colour. */
    public static TextureData solidColor(int r, int g, int b, int a) {
        byte[] rgba = { (byte) r, (byte) g, (byte) b, (byte) a };
        return new TextureData(1, 1, rgba, TextureColorSpace.LINEAR);
    }
}
