package com.simplerender.memory;

public final class MemoryBlock {
    private final byte[] buffer;
    private final int offset;
    private final int size;

    MemoryBlock(byte[] buffer, int offset, int size) {
        this.buffer = buffer;
        this.offset = offset;
        this.size = size;
    }

    public byte[] buffer() {
        return buffer;
    }

    public int offset() {
        return offset;
    }

    public int size() {
        return size;
    }
}
