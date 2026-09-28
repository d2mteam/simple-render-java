package com.simplerender.engine;

import com.simplerender.render.ShaderLibrary;

/**
 * Start-up settings for the {@link Engine}.
 *
 * @param targetFps   frame rate cap; 0 or less renders as fast as possible
 * @param maxFrames   stop after this many frames; 0 or less runs until {@link Engine#stop()}
 * @param shaderName  initial scene shader, one of {@link ShaderLibrary#SCENE_SHADERS}
 */
public record EngineConfig(int targetFps, int maxFrames, String shaderName) {

    public static EngineConfig defaults() {
        return new EngineConfig(60, 0, ShaderLibrary.DEFAULT_SCENE_SHADER);
    }

    public EngineConfig withShaderName(String shaderName) {
        return new EngineConfig(targetFps, maxFrames, shaderName);
    }

    public EngineConfig withMaxFrames(int maxFrames) {
        return new EngineConfig(targetFps, maxFrames, shaderName);
    }
}
