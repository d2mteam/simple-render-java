package com.simplerender.app;

public final class EngineConfig {
    private final int targetFps;
    private final int maxFrames;
    private final int chunkCount;
    private final long randomSeed;
    private final String modelPath;
    private final String shaderName;

    public EngineConfig(
        int targetFps,
        int maxFrames,
        int chunkCount,
        long randomSeed,
        String modelPath,
        String shaderName
    ) {
        this.targetFps = targetFps;
        this.maxFrames = maxFrames;
        this.chunkCount = chunkCount;
        this.randomSeed = randomSeed;
        this.modelPath = modelPath;
        this.shaderName = shaderName;
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

    public String modelPath() {
        return modelPath;
    }

    public String shaderName() {
        return shaderName;
    }

    public EngineConfig withModelPath(String modelPath) {
        return new EngineConfig(targetFps, maxFrames, chunkCount, randomSeed, modelPath, shaderName);
    }

    public EngineConfig withShaderName(String shaderName) {
        return new EngineConfig(targetFps, maxFrames, chunkCount, randomSeed, modelPath, shaderName);
    }

    public static EngineConfig defaultConfig() {
        return new EngineConfig(60, 3, 3, 1337L, null, "default");
    }
}
