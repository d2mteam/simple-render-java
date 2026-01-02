package com.simplerender.memory;

import java.util.ArrayDeque;
import java.util.Deque;

public final class TlsfAllocator implements Allocator {
    private static final int MIN_BLOCK = 16;
    private final byte[] buffer;
    private final Deque<FreeBlock>[] buckets;

    @SuppressWarnings("unchecked")
    public TlsfAllocator(int capacity, int bucketCount) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.buffer = new byte[capacity];
        this.buckets = new Deque[bucketCount];
        for (int i = 0; i < bucketCount; i++) {
            buckets[i] = new ArrayDeque<>();
        }
        buckets[bucketCount - 1].add(new FreeBlock(0, capacity));
    }

    @Override
    public MemoryBlock allocate(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }
        int request = Math.max(size, MIN_BLOCK);
        int bucket = bucketIndex(request);
        FreeBlock block = findBlock(bucket, request);
        if (block == null) {
            throw new IllegalStateException("TLSF allocator out of memory");
        }
        int offset = block.offset;
        int remaining = block.size - request;
        if (remaining > MIN_BLOCK) {
            FreeBlock split = new FreeBlock(offset + request, remaining);
            buckets[bucketIndex(split.size)].add(split);
        }
        return new MemoryBlock(buffer, offset, request);
    }

    @Override
    public void free(MemoryBlock block) {
        if (block == null) {
            return;
        }
        if (block.buffer() != buffer) {
            throw new IllegalArgumentException("Block not owned by this allocator");
        }
        buckets[bucketIndex(block.size())].add(new FreeBlock(block.offset(), block.size()));
    }

    private FreeBlock findBlock(int startBucket, int size) {
        for (int i = startBucket; i < buckets.length; i++) {
            FreeBlock candidate = buckets[i].poll();
            if (candidate != null) {
                if (candidate.size >= size) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private int bucketIndex(int size) {
        int bucket = 0;
        int value = MIN_BLOCK;
        while (value < size && bucket < buckets.length - 1) {
            value <<= 1;
            bucket++;
        }
        return bucket;
    }

    private static final class FreeBlock {
        private final int offset;
        private final int size;

        private FreeBlock(int offset, int size) {
            this.offset = offset;
            this.size = size;
        }
    }
}
