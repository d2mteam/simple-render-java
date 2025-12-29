package com.simplerender.app;

import com.simplerender.scene.Scene;
import com.simplerender.gl.OpenGLRenderer;

public final class GameLoop {
    private final EngineConfig config;
    private final Time time;
    private final Scene scene;
    private final OpenGLRenderer renderer;

    public GameLoop(EngineConfig config, Time time, Scene scene, OpenGLRenderer renderer) {
        this.config = config;
        this.time = time;
        this.scene = scene;
        this.renderer = renderer;
    }

    public void run() {
        int frame = 0;
        while (frame < config.maxFrames()) {
            time.update();
            scene.update(time);
            renderer.render(scene.snapshot());
            frame++;
        }
    }
}
