package com.simplerender.memory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public final class BuddyAllocator implements Allocator {
    private final byte[] buffer;
    private final int maxOrder;
    private final Deque<Integer>[] freeLists;
    private final Map<Integer, Integer> allocations = new HashMap<>();

    @SuppressWarnings("unchecked")
    public BuddyAllocator(int totalSize) {
        if (totalSize <= 0 || (totalSize & (totalSize - 1)) != 0) {
            throw new IllegalArgumentException("Total size must be power of two");
        }
        this.buffer = new byte[totalSize];
        this.maxOrder = Integer.numberOfTrailingZeros(totalSize);
        this.freeLists = new Deque[maxOrder + 1];
        for (int i = 0; i <= maxOrder; i++) {
            freeLists[i] = new ArrayDeque<>();
        }
        freeLists[maxOrder].push(0);
    }

    @Override
    public MemoryBlock allocate(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }
        int order = requiredOrder(size);
        int currentOrder = order;
        while (currentOrder <= maxOrder && freeLists[currentOrder].isEmpty()) {
            currentOrder++;
        }
        if (currentOrder > maxOrder) {
            throw new IllegalStateException("Buddy allocator out of memory");
        }
        int offset = freeLists[currentOrder].pop();
        while (currentOrder > order) {
            currentOrder--;
            int buddyOffset = offset + (1 << currentOrder);
            freeLists[currentOrder].push(buddyOffset);
        }
        allocations.put(offset, order);
        int allocSize = 1 << order;
        return new MemoryBlock(buffer, offset, Math.min(size, allocSize));
    }

    @Override
    public void free(MemoryBlock block) {
        if (block == null) {
            return;
        }
        if (block.buffer() != buffer) {
            throw new IllegalArgumentException("Block not owned by this allocator");
        }
        Integer order = allocations.remove(block.offset());
        if (order == null) {
            throw new IllegalArgumentException("Unknown block");
        }
        int offset = block.offset();
        int currentOrder = order;
        while (currentOrder < maxOrder) {
            int buddyOffset = offset ^ (1 << currentOrder);
            if (!freeLists[currentOrder].remove(buddyOffset)) {
                break;
            }
            offset = Math.min(offset, buddyOffset);
            currentOrder++;
        }
        freeLists[currentOrder].push(offset);
    }

    private int requiredOrder(int size) {
        int order = 0;
        int value = 1;
        while (value < size) {
            value <<= 1;
            order++;
        }
        return order;
    }
}
