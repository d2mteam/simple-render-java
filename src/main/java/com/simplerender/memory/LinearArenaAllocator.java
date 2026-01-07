package com.simplerender.memory;

/**
 * Linear arena allocator that resets in bulk.
 */
public final class LinearArenaAllocator implements Allocator {
    private final byte[] buffer;
    private int offset;

    public LinearArenaAllocator(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.buffer = new byte[capacity];
    }

    @Override
    public MemoryBlock allocate(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }
        if (offset + size > buffer.length) {
            throw new IllegalStateException("Arena out of memory");
        }
        MemoryBlock block = new MemoryBlock(buffer, offset, size);
        offset += size;
        return block;
    }

    @Override
    public void free(MemoryBlock block) {
        if (block == null) {
            return;
        }
        if (block.buffer() != buffer) {
            throw new IllegalArgumentException("Block not owned by this arena");
        }
    }

    public void reset() {
        offset = 0;
    }

    public int used() {
        return offset;
    }

    public int capacity() {
        return buffer.length;
    }
}
