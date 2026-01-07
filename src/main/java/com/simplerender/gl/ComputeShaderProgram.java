package com.simplerender.gl;

import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL43;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper for compiling, linking, and executing compute shaders.
 *
 * <p>Provides convenience methods for binding and setting common uniforms.
 */
final class ComputeShaderProgram {
    private static final Logger logger = LoggerFactory.getLogger(ComputeShaderProgram.class);

    private int programId;
    private boolean initialized;

    public void init(String computeSource) {
        int computeShader = compileShader(GL43.GL_COMPUTE_SHADER, computeSource);
        programId = GL20.glCreateProgram();
        GL20.glAttachShader(programId, computeShader);
        GL20.glLinkProgram(programId);
        int linked = GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS);
        if (linked == 0) {
            logger.error("Compute shader link failed: {}", GL20.glGetProgramInfoLog(programId));
            throw new IllegalStateException("Compute shader link failed");
        }
        GL20.glDetachShader(programId, computeShader);
        GL20.glDeleteShader(computeShader);
        initialized = true;
        logger.info("Compute shader program initialized");
    }

    public void bind() {
        GL20.glUseProgram(programId);
    }

    public void setUniformInt(String name, int value) {
        int location = GL20.glGetUniformLocation(programId, name);
        if (location < 0) {
            logger.error("Uniform {} not found", name);
            return;
        }
        GL20.glUniform1i(location, value);
    }

    public void setUniformFloat(String name, float value) {
        int location = GL20.glGetUniformLocation(programId, name);
        if (location < 0) {
            logger.error("Uniform {} not found", name);
            return;
        }
        GL20.glUniform1f(location, value);
    }

    public void setUniformVec2(String name, float[] vec2) {
        int location = GL20.glGetUniformLocation(programId, name);
        if (location < 0) {
            logger.error("Uniform {} not found", name);
            return;
        }
        GL20.glUniform2f(location, vec2[0], vec2[1]);
    }

    public void dispatch(int groupX, int groupY, int groupZ) {
        GL43.glDispatchCompute(groupX, groupY, groupZ);
    }

    public boolean isInitialized() {
        return initialized;
    }

    private int compileShader(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        int compiled = GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS);
        if (compiled == 0) {
            logger.error("Compute shader compile failed: {}", GL20.glGetShaderInfoLog(shader));
            throw new IllegalStateException("Compute shader compile failed");
        }
        return shader;
    }
}
