package com.simplerender.memory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SlabAllocator implements Allocator {
    private final Map<Integer, PoolAllocator> slabs = new LinkedHashMap<>();

    public SlabAllocator(int... slabSizes) {
        for (int size : slabSizes) {
            if (size <= 0) {
                throw new IllegalArgumentException("Slab sizes must be positive");
            }
            slabs.put(size, new PoolAllocator(size, 128));
        }
        if (slabs.isEmpty()) {
            throw new IllegalArgumentException("At least one slab size is required");
        }
    }

    @Override
    public MemoryBlock allocate(int size) {
        PoolAllocator pool = findPool(size);
        return pool.allocate(size);
    }

    @Override
    public void free(MemoryBlock block) {
        if (block == null) {
            return;
        }
        PoolAllocator pool = findPool(block.size());
        pool.free(block);
    }

    private PoolAllocator findPool(int size) {
        for (Map.Entry<Integer, PoolAllocator> entry : slabs.entrySet()) {
            if (size <= entry.getKey()) {
                return entry.getValue();
            }
        }
        throw new IllegalArgumentException("No slab available for size " + size);
    }
}
