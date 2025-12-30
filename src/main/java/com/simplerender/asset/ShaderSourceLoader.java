package com.simplerender.asset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class ShaderSourceLoader {
    private static final Logger logger = LoggerFactory.getLogger(ShaderSourceLoader.class);

    private ShaderSourceLoader() {
    }

    public static ShaderSource load(String vertexPath, String fragmentPath) {
        String vertexSource = readResource(vertexPath);
        String fragmentSource = readResource(fragmentPath);
        logger.info("Loaded shader sources: vertex={}, fragment={}", vertexPath, fragmentPath);
        return new ShaderSource(vertexSource, fragmentSource);
    }

    private static String readResource(String resourcePath) {
        try (InputStream input = ShaderSourceLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Shader resource not found: " + resourcePath);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read shader resource: " + resourcePath, e);
        }
    }
}
