package com.simplerender.memory;

import java.util.ArrayDeque;
import java.util.Deque;

public final class StackAllocator implements Allocator {
    private final byte[] buffer;
    private int offset;
    private final Deque<Integer> markers = new ArrayDeque<>();

    public StackAllocator(int capacity) {
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
            throw new IllegalStateException("Stack allocator out of memory");
        }
        markers.push(offset);
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
            throw new IllegalArgumentException("Block not owned by this allocator");
        }
        if (markers.isEmpty()) {
            throw new IllegalStateException("Stack allocator underflow");
        }
        int previousOffset = markers.pop();
        if (block.offset() != previousOffset) {
            throw new IllegalStateException("Stack allocator must free in LIFO order");
        }
        offset = previousOffset;
    }

    public int used() {
        return offset;
    }

    public int capacity() {
        return buffer.length;
    }
}
