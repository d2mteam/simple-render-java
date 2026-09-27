package com.simplerender.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.simplerender.engine.EngineConfig;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class LaunchOptionsTest {
    @Test
    void noArgumentsGiveDefaults() {
        LaunchOptions options = LaunchOptions.parse(List.of());
        assertEquals(EngineConfig.defaults(), options.engineConfig());
        assertNull(options.modelPath());
    }

    @Test
    void acceptsBothOptionStyles() {
        LaunchOptions options = LaunchOptions.parse(
                List.of("--model", "test.obj", "--shader=disney_brdf", "--max-frames", "10"));
        assertEquals(Path.of("test.obj"), options.modelPath());
        assertEquals("disney_brdf", options.engineConfig().shaderName());
        assertEquals(10, options.engineConfig().maxFrames());
    }

    @Test
    void ignoresUnknownAndBlankOptions() {
        LaunchOptions options = LaunchOptions.parse(List.of("--model=", "--colour", "red", "stray"));
        assertNull(options.modelPath());
        assertEquals(EngineConfig.defaults(), options.engineConfig());
    }
}
