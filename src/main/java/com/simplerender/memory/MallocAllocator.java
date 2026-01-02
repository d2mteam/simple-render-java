package com.simplerender.memory;

public final class MallocAllocator implements Allocator {
    @Override
    public MemoryBlock allocate(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }
        return new MemoryBlock(new byte[size], 0, size);
    }

    @Override
    public void free(MemoryBlock block) {
        // GC-managed in Java, explicit free is a no-op.
    }
}
