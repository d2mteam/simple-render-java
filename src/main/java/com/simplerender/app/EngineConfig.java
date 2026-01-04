package com.simplerender.app;

public final class EngineConfig {
    private final int targetFps;
    private final int maxFrames;
    private final String modelPath;
    private final String shaderName;

    public EngineConfig(
            int targetFps,
            int maxFrames,
            String modelPath,
            String shaderName) {
        this.targetFps = targetFps;
        this.maxFrames = maxFrames;
        this.modelPath = modelPath;
        this.shaderName = shaderName;
    }

    public int targetFps() {
        return targetFps;
    }

    public int maxFrames() {
        return maxFrames;
    }

    public String modelPath() {
        return modelPath;
    }

    public String shaderName() {
        return shaderName;
    }

    public EngineConfig withModelPath(String modelPath) {
        return new EngineConfig(targetFps, maxFrames, modelPath, shaderName);
    }

    public EngineConfig withShaderName(String shaderName) {
        return new EngineConfig(targetFps, maxFrames, modelPath, shaderName);
    }

    public static EngineConfig defaultConfig() {
        return new EngineConfig(60, -1, null, "default");
    }
}
