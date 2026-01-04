package com.simplerender.asset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class ShaderSourceLoader {
    private static final Logger logger = LoggerFactory.getLogger(ShaderSourceLoader.class);
    private static final String DEFAULT_SHADER = "default";
    private static final String DISNEY_BRDF_SHADER = "disney_brdf";

    private ShaderSourceLoader() {
    }

    public static ShaderSource loadByName(String shaderName) {
        String resolved = shaderName == null || shaderName.isBlank() ? DEFAULT_SHADER : shaderName;
        return switch (resolved) {
            case DEFAULT_SHADER -> load("shaders/default.vert", "shaders/default.frag");
            case DISNEY_BRDF_SHADER -> load("shaders/disney_brdf.vert", "shaders/disney_brdf.frag");
            case "debug_mesh" -> load("shaders/debug_mesh.vert", "shaders/debug_mesh.frag");
            default -> {
                logger.warn("Unknown shader '{}', falling back to {}", resolved, DEFAULT_SHADER);
                yield load("shaders/default.vert", "shaders/default.frag");
            }
        };
    }

    public static ShaderSource load(String vertexPath, String fragmentPath) {
        String vertexSource = readResource(vertexPath);
        String fragmentSource = readResource(fragmentPath);
        logger.info("Loaded shader sources: vertex={}, fragment={}", vertexPath, fragmentPath);
        return new ShaderSource(vertexSource, fragmentSource);
    }

    public static String loadCompute(String computePath) {
        String computeSource = readResource(computePath);
        logger.info("Loaded compute shader source: {}", computePath);
        return computeSource;
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
