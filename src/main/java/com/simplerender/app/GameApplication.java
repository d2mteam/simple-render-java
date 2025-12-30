package com.simplerender.app;

import com.simplerender.gl.OpenGLRenderer;
import com.simplerender.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GameApplication {
    private static final Logger logger = LoggerFactory.getLogger(GameApplication.class);

    private final EngineConfig config;
    private final GameLoop gameLoop;
    private final Scene scene;
    private final OpenGLRenderer renderer;

    public GameApplication(EngineConfig config) {
        this.config = config;
        this.renderer = new OpenGLRenderer(config.chunkCount());
        this.scene = Scene.bootstrap(config, renderer);
        this.gameLoop = new GameLoop(config, new Time(), scene, renderer);
    }

    public void run() {
        logger.info("GameApplication run started");
        gameLoop.run();
        logger.info("GameApplication run completed");
    }
}
