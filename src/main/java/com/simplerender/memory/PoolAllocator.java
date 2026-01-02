package com.simplerender.memory;

import java.util.ArrayDeque;
import java.util.Deque;

public final class PoolAllocator implements Allocator {
    private final byte[] buffer;
    private final int blockSize;
    private final Deque<Integer> freeList = new ArrayDeque<>();

    public PoolAllocator(int blockSize, int blockCount) {
        if (blockSize <= 0 || blockCount <= 0) {
            throw new IllegalArgumentException("Block size and count must be positive");
        }
        this.blockSize = blockSize;
        this.buffer = new byte[blockSize * blockCount];
        for (int i = 0; i < blockCount; i++) {
            freeList.push(i * blockSize);
        }
    }

    @Override
    public MemoryBlock allocate(int size) {
        if (size <= 0 || size > blockSize) {
            throw new IllegalArgumentException("Size must be within pool block size");
        }
        if (freeList.isEmpty()) {
            throw new IllegalStateException("Pool allocator out of blocks");
        }
        int offset = freeList.pop();
        return new MemoryBlock(buffer, offset, size);
    }

    @Override
    public void free(MemoryBlock block) {
        if (block == null) {
            return;
        }
        if (block.buffer() != buffer) {
            throw new IllegalArgumentException("Block not owned by this pool");
        }
        if (block.offset() % blockSize != 0) {
            throw new IllegalArgumentException("Invalid pool block offset");
        }
        freeList.push(block.offset());
    }

    public int blockSize() {
        return blockSize;
    }

    public int capacity() {
        return buffer.length;
    }
}
