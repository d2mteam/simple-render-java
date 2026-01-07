package com.simplerender.app;

import com.simplerender.scene.Scene;
import com.simplerender.gl.OpenGLRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main update/render loop driving simulation timing and drawing.
 */
public final class GameLoop {
    private static final Logger logger = LoggerFactory.getLogger(GameLoop.class);

    private final EngineConfig config;
    private final Time time;
    private final Scene scene;
    private final OpenGLRenderer renderer;
    private final JavaFxInputAdapter inputAdapter;

    public GameLoop(EngineConfig config, Time time, Scene scene, OpenGLRenderer renderer,
            JavaFxInputAdapter inputAdapter) {
        this.config = config;
        this.time = time;
        this.scene = scene;
        this.renderer = renderer;
        this.inputAdapter = inputAdapter;
    }

    public void run() {
        int frame = 0;
        boolean limitFrames = config.maxFrames() > 0;
        long frameDurationMs = 0L;
        if (config.targetFps() > 0) {
            frameDurationMs = 1000L / config.targetFps();
        }
        if (!limitFrames) {
            logger.info("Game loop starting with continuous run");
        } else {
            logger.info("Game loop starting with maxFrames={}", config.maxFrames());
        }
        while (!renderer.shouldClose()) {
            long frameStartNs = System.nanoTime();
            time.update();
            scene.update(time, inputAdapter.consumeInput());
            renderer.render(scene.snapshot());
            if (frameDurationMs > 0) {
                long frameElapsedNs = System.nanoTime() - frameStartNs;
                long frameDurationNs = frameDurationMs * 1_000_000L;
                long remainingNs = frameDurationNs - frameElapsedNs;
                if (remainingNs > 0) {
                    long sleepMs = remainingNs / 1_000_000L;
                    int sleepNs = (int) (remainingNs % 1_000_000L);
                    try {
                        Thread.sleep(sleepMs, sleepNs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.warn("Game loop interrupted during frame cap sleep", e);
                        break;
                    }
                }
            }
            frame++;
            if (config.maxFrames() > 0 && frame >= config.maxFrames()) {
                logger.info("Max frames reached, stopping loop");
                renderer.requestStop();
            }
        }
        logger.info("Game loop completed after {} frames", frame);
    }
}
