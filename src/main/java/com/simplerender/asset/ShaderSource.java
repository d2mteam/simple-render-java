package com.simplerender.asset;

/**
 * Pair of vertex and fragment shader sources as UTF-8 strings.
 */
public record ShaderSource(String vertexSource, String fragmentSource) {
}
