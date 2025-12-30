package com.simplerender.asset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class ShaderSourceLoader {
    private static final Logger logger = LoggerFactory.getLogger(ShaderSourceLoader.class);
    private static final String DEFAULT_SHADER = "default";
    private static final String SHADER_DIRECTORY = "shaders/";

    private ShaderSourceLoader() {
    }

    public static ShaderSource loadByName(String shaderName) {
        String resolved = shaderName == null || shaderName.isBlank() ? DEFAULT_SHADER : shaderName;
        String vertexPath = shaderPath(resolved, "vert");
        String fragmentPath = shaderPath(resolved, "frag");
        if (resourcesExist(vertexPath, fragmentPath)) {
            return load(vertexPath, fragmentPath);
        }
        if (!resolved.equals(DEFAULT_SHADER)) {
            logger.warn("Unknown shader '{}', falling back to {}", resolved, DEFAULT_SHADER);
        }
        return load(shaderPath(DEFAULT_SHADER, "vert"), shaderPath(DEFAULT_SHADER, "frag"));
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

    private static String shaderPath(String shaderName, String extension) {
        return SHADER_DIRECTORY + shaderName + "." + extension;
    }

    private static boolean resourcesExist(String vertexPath, String fragmentPath) {
        ClassLoader classLoader = ShaderSourceLoader.class.getClassLoader();
        return classLoader.getResource(vertexPath) != null && classLoader.getResource(fragmentPath) != null;
    }
}
