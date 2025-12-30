package com.simplerender.asset;

public final class TextureDataFactory {
    private TextureDataFactory() {
    }

    public static TextureData checkerboard(int size, int cellSize) {
        int width = size;
        int height = size;
        byte[] rgba = new byte[width * height * 4];
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean even = ((x / cellSize) + (y / cellSize)) % 2 == 0;
                byte value = (byte) (even ? 220 : 60);
                rgba[index++] = value;
                rgba[index++] = value;
                rgba[index++] = value;
                rgba[index++] = (byte) 255;
            }
        }
        return new TextureData(width, height, rgba);
    }
}
