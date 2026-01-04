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
        String shaderName = parseArg(args, "--shader");
        if (shaderName != null) {
            config = config.withShaderName(shaderName);
        }
        String modelPath = parseArg(args, "--model");
        if (modelPath != null) {
            config = config.withModelPath(modelPath);
        }
        logger.info(
                "Starting Simple Render (shader={})",
                config.shaderName());
        GameApplication application = new GameApplication(config);
        application.run();
    }

    private static String parseArg(String[] args, String key) {
        if (args == null || args.length == 0) {
            return null;
        }
        String keyEq = key + "=";
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg == null) {
                continue;
            }
            if (arg.startsWith(keyEq)) {
                String value = arg.substring(keyEq.length());
                return value.isBlank() ? null : value;
            }
            if (arg.equals(key) && i + 1 < args.length) {
                String value = args[i + 1];
                return value == null || value.isBlank() ? null : value;
            }
        }
        return null;
    }
}
