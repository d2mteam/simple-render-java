package com.simplerender.memory;

/**
 * Simple allocator that returns new byte arrays per allocation.
 *
 * <p>Useful as a baseline allocator in tests and examples.
 */
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
