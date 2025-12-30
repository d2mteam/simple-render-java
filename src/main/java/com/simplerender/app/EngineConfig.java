package com.simplerender.app;

public final class EngineConfig {
    private final int targetFps;
    private final int maxFrames;
    private final int chunkCount;
    private final long randomSeed;

    public EngineConfig(int targetFps, int maxFrames, int chunkCount, long randomSeed) {
        this.targetFps = targetFps;
        this.maxFrames = maxFrames;
        this.chunkCount = chunkCount;
        this.randomSeed = randomSeed;
    }

    public int targetFps() {
        return targetFps;
    }

    public int maxFrames() {
        return maxFrames;
    }

    public int chunkCount() {
        return chunkCount;
    }

    public long randomSeed() {
        return randomSeed;
    }

    public static EngineConfig defaultConfig() {
        return new EngineConfig(60, 3, 3, 1337L);
    }
}
