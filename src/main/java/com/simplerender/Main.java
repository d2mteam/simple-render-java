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
        String shaderName = parseShaderName(args);
        if (shaderName != null) {
            config = config.withShaderName(shaderName);
        }
        logger.info(
            "Starting Simple Render with {} chunks (seed={}, shader={})",
            config.chunkCount(),
            config.randomSeed(),
            config.shaderName()
        );
        GameApplication application = new GameApplication(config);
        application.run();
    }

    private static String parseShaderName(String[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg == null) {
                continue;
            }
            if (arg.startsWith("--shader=")) {
                String value = arg.substring("--shader=".length());
                return value.isBlank() ? null : value;
            }
            if (arg.equals("--shader") && i + 1 < args.length) {
                String value = args[i + 1];
                return value == null || value.isBlank() ? null : value;
            }
        }
        return null;
    }
}
