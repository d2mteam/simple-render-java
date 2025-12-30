package com.simplerender;

import com.simplerender.app.EngineConfig;
import com.simplerender.app.GameApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private Main() {
    }

    public static void main(String[] args) {
        EngineConfig config = EngineConfig.defaultConfig();
        logger.info("Starting Simple Render with {} chunks (seed={})", config.chunkCount(), config.randomSeed());
        GameApplication application = new GameApplication(config);
        application.run();
    }
}
