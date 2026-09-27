package com.simplerender.render;

import com.simplerender.render.gl.ShaderProgram;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The GLSL programs shipped in {@code engine/src/main/resources/shaders}.
 *
 * <p>A scene shader named {@code foo} is the pair {@code shaders/foo.vert} +
 * {@code shaders/foo.frag}. To add one, drop the two files in and list the name in
 * {@link #SCENE_SHADERS}.
 */
public final class ShaderLibrary {
    private static final Logger logger = LoggerFactory.getLogger(ShaderLibrary.class);

    public static final String DEFAULT_SCENE_SHADER = "default";

    /** Shaders that can draw the scene; they all read the same vertex layout and uniforms. */
    public static final List<String> SCENE_SHADERS = List.of(DEFAULT_SCENE_SHADER, "disney_brdf", "debug_mesh");

    private ShaderLibrary() {
    }

    /** Returns {@code name} if it is a known scene shader, otherwise the default one. */
    public static String resolveSceneShader(String name) {
        if (SCENE_SHADERS.contains(name)) {
            return name;
        }
        logger.warn("Unknown shader '{}', using '{}'", name, DEFAULT_SCENE_SHADER);
        return DEFAULT_SCENE_SHADER;
    }

    static ShaderProgram loadSceneShader(String name) {
        String resolved = resolveSceneShader(name);
        return load("shaders/" + resolved + ".vert", "shaders/" + resolved + ".frag");
    }

    static ShaderProgram loadPostProcessShader() {
        return load("shaders/screen_post.vert", "shaders/screen_post.frag");
    }

    private static ShaderProgram load(String vertexPath, String fragmentPath) {
        logger.info("Compiling shader {} + {}", vertexPath, fragmentPath);
        return new ShaderProgram(readResource(vertexPath), readResource(fragmentPath));
    }

    private static String readResource(String path) {
        try (InputStream input = ShaderLibrary.class.getClassLoader().getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("Shader resource not found: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read shader resource: " + path, e);
        }
    }
}
