package com.simplerender.app;

public final class EngineConfig {
    private final int targetFps;
    private final int maxFrames;

    public EngineConfig(int targetFps, int maxFrames) {
        this.targetFps = targetFps;
        this.maxFrames = maxFrames;
    }

    public int targetFps() {
        return targetFps;
    }

    public int maxFrames() {
        return maxFrames;
    }

    public static EngineConfig defaultConfig() {
        return new EngineConfig(60, 3);
    }
}
