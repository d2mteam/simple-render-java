package com.simplerender.app;

import com.simplerender.scene.Scene;
import com.simplerender.gl.OpenGLRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GameLoop {
    private static final Logger logger = LoggerFactory.getLogger(GameLoop.class);

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
        boolean limitFrames = config.maxFrames() > 0;
        if (!limitFrames) {
            logger.info("Game loop starting with continuous run");
        } else {
            logger.info("Game loop starting with maxFrames={}", config.maxFrames());
        }
        while (!renderer.shouldClose()) {
            time.update();
            renderer.pollEvents();
            scene.update(time, renderer.readInput());
            renderer.render(scene.snapshot());
            logger.debug("Completed frame {}", frame);
            frame++;
        }
        logger.info("Game loop completed after {} frames", frame);
    }
}
