package com.simplerender.ui;

import com.simplerender.engine.EngineConfig;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command-line options, see {@link Main}.
 *
 * @param modelPath model to load at start-up, or {@code null}
 */
record LaunchOptions(EngineConfig engineConfig, Path modelPath) {
    private static final Logger logger = LoggerFactory.getLogger(LaunchOptions.class);

    static LaunchOptions parse(List<String> args) {
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            int equals = arg.indexOf('=');
            if (!arg.startsWith("--")) {
                logger.warn("Ignoring argument '{}'", arg);
            } else if (equals >= 0) {
                values.put(arg.substring(0, equals), arg.substring(equals + 1));
            } else if (i + 1 < args.size()) {
                values.put(arg, args.get(++i));
            } else {
                logger.warn("Missing value for '{}'", arg);
            }
        }

        EngineConfig config = EngineConfig.defaults();
        String shader = values.remove("--shader");
        if (isSet(shader)) {
            config = config.withShaderName(shader);
        }
        String maxFrames = values.remove("--max-frames");
        if (isSet(maxFrames)) {
            config = config.withMaxFrames(Integer.parseInt(maxFrames.trim()));
        }
        String model = values.remove("--model");
        values.keySet().forEach(unknown -> logger.warn("Unknown option '{}'", unknown));
        return new LaunchOptions(config, isSet(model) ? Path.of(model) : null);
    }

    private static boolean isSet(String value) {
        return value != null && !value.isBlank();
    }
}
