package com.simplerender.memory;

public interface Allocator {
    MemoryBlock allocate(int size);

    void free(MemoryBlock block);
}
