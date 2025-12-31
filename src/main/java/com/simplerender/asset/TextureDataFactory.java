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

    public static TextureData solidColor(int r, int g, int b, int a) {
        byte[] rgba = new byte[4];
        rgba[0] = (byte) r;
        rgba[1] = (byte) g;
        rgba[2] = (byte) b;
        rgba[3] = (byte) a;
        return new TextureData(1, 1, rgba);
    }
}
