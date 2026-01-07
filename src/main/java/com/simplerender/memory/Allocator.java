package com.simplerender.memory;

/**
 * Contract for custom memory allocators used in engine experiments.
 */
public interface Allocator {
    MemoryBlock allocate(int size);

    void free(MemoryBlock block);
}
