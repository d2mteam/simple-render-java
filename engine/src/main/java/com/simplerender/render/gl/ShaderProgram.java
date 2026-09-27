package com.simplerender.render.gl;

import java.util.HashMap;
import java.util.Map;
import org.lwjgl.opengl.GL20;

/**
 * A compiled and linked vertex + fragment shader.
 *
 * <p>Uniform setters silently ignore names the shader does not declare (or that the GLSL
 * compiler optimised away), so one renderer can drive shaders that use different subsets
 * of the uniforms. Call {@link #use()} before setting uniforms.
 */
public final class ShaderProgram implements AutoCloseable {
    private final int programId;
    private final Map<String, Integer> uniformLocations = new HashMap<>();

    public ShaderProgram(String vertexSource, String fragmentSource) {
        int vertexShader = compile(GL20.GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = compile(GL20.GL_FRAGMENT_SHADER, fragmentSource);
        programId = GL20.glCreateProgram();
        GL20.glAttachShader(programId, vertexShader);
        GL20.glAttachShader(programId, fragmentShader);
        GL20.glLinkProgram(programId);
        GL20.glDetachShader(programId, vertexShader);
        GL20.glDetachShader(programId, fragmentShader);
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
        if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == 0) {
            String log = GL20.glGetProgramInfoLog(programId);
            GL20.glDeleteProgram(programId);
            throw new IllegalStateException("Shader link failed:\n" + log);
        }
    }

    public void use() {
        GL20.glUseProgram(programId);
    }

    public void setInt(String name, int value) {
        int location = location(name);
        if (location >= 0) {
            GL20.glUniform1i(location, value);
        }
    }

    public void setFloat(String name, float value) {
        int location = location(name);
        if (location >= 0) {
            GL20.glUniform1f(location, value);
        }
    }

    public void setVec2(String name, float x, float y) {
        int location = location(name);
        if (location >= 0) {
            GL20.glUniform2f(location, x, y);
        }
    }

    public void setVec3(String name, float[] xyz) {
        int location = location(name);
        if (location >= 0) {
            GL20.glUniform3f(location, xyz[0], xyz[1], xyz[2]);
        }
    }

    public void setVec4(String name, float x, float y, float z, float w) {
        int location = location(name);
        if (location >= 0) {
            GL20.glUniform4f(location, x, y, z, w);
        }
    }

    /** Sets a 4x4 matrix given in column-major order. */
    public void setMat4(String name, float[] matrix) {
        int location = location(name);
        if (location >= 0) {
            GL20.glUniformMatrix4fv(location, false, matrix);
        }
    }

    @Override
    public void close() {
        GL20.glDeleteProgram(programId);
    }

    private int location(String name) {
        return uniformLocations.computeIfAbsent(name, n -> GL20.glGetUniformLocation(programId, n));
    }

    private static int compile(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0) {
            String log = GL20.glGetShaderInfoLog(shader);
            GL20.glDeleteShader(shader);
            String stage = type == GL20.GL_VERTEX_SHADER ? "Vertex" : "Fragment";
            throw new IllegalStateException(stage + " shader compile failed:\n" + log);
        }
        return shader;
    }
}
