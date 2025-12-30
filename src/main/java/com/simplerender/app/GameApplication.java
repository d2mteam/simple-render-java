package com.simplerender.app;

import com.simplerender.gl.OpenGLRenderer;
import com.simplerender.scene.Scene;

public final class GameApplication {
    private final EngineConfig config;
    private final GameLoop gameLoop;
    private final Scene scene;
    private final OpenGLRenderer renderer;

    public GameApplication(EngineConfig config) {
        this.config = config;
        this.scene = Scene.bootstrap(config);
        this.renderer = new OpenGLRenderer(config.chunkCount());
        this.gameLoop = new GameLoop(config, new Time(), scene, renderer);
    }

    public void run() {
        gameLoop.run();
    }
}
