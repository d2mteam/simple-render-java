package com.simplerender.world;

public final class ChunkBlockView {
    private final int size;
    private final byte[] blocks;

    public ChunkBlockView(int size, byte[] blocks) {
        this.size = size;
        this.blocks = blocks;
    }

    public int size() {
        return size;
    }

    public byte blockAt(int index) {
        return blocks[index];
    }
}
