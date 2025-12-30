package com.simplerender.gl;

import org.lwjgl.opengl.GL20;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;

final class ShaderProgram {
    private static final Logger logger = LoggerFactory.getLogger(ShaderProgram.class);

    private int programId;
    private boolean initialized;
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    public void init(String vertexSource, String fragmentSource) {
        int vertexShader = compileShader(GL20.GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = compileShader(GL20.GL_FRAGMENT_SHADER, fragmentSource);
        programId = GL20.glCreateProgram();
        GL20.glAttachShader(programId, vertexShader);
        GL20.glAttachShader(programId, fragmentShader);
        GL20.glLinkProgram(programId);
        int linked = GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS);
        if (linked == 0) {
            logger.error("Shader link failed: {}", GL20.glGetProgramInfoLog(programId));
            throw new IllegalStateException("Shader program link failed");
        }
        GL20.glDetachShader(programId, vertexShader);
        GL20.glDetachShader(programId, fragmentShader);
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
        initialized = true;
        logger.info("Shader program initialized");
    }

    public void bind() {
        GL20.glUseProgram(programId);
    }

    public void setUniformMat4(String name, float[] matrix) {
        int location = GL20.glGetUniformLocation(programId, name);
        if (location < 0) {
            logger.error("Uniform {} not found", name);
            return;
        }
        matrixBuffer.clear();
        matrixBuffer.put(matrix).flip();
        GL20.glUniformMatrix4fv(location, false, matrixBuffer);
    }

    public void setUniformVec3(String name, float[] vec3) {
        int location = GL20.glGetUniformLocation(programId, name);
        if (location < 0) {
            logger.error("Uniform {} not found", name);
            return;
        }
        GL20.glUniform3f(location, vec3[0], vec3[1], vec3[2]);
    }

    public void setUniformInt(String name, int value) {
        int location = GL20.glGetUniformLocation(programId, name);
        if (location < 0) {
            logger.error("Uniform {} not found", name);
            return;
        }
        GL20.glUniform1i(location, value);
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
            logger.error("Shader compile failed: {}", GL20.glGetShaderInfoLog(shader));
            throw new IllegalStateException("Shader compile failed");
        }
        return shader;
    }
}
