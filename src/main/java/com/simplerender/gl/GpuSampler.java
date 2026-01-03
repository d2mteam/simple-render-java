package com.simplerender.gl;

import com.simplerender.asset.SamplerData;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL33;

final class GpuSampler {
    private final int samplerId;

    GpuSampler(SamplerData samplerData) {
        samplerId = GL33.glGenSamplers();
        GL33.glSamplerParameteri(samplerId, GL11.GL_TEXTURE_MIN_FILTER, normalizeMinFilter(samplerData.minFilter()));
        GL33.glSamplerParameteri(samplerId, GL11.GL_TEXTURE_MAG_FILTER, normalizeMagFilter(samplerData.magFilter()));
        GL33.glSamplerParameteri(samplerId, GL11.GL_TEXTURE_WRAP_S, normalizeWrap(samplerData.wrapS()));
        GL33.glSamplerParameteri(samplerId, GL11.GL_TEXTURE_WRAP_T, normalizeWrap(samplerData.wrapT()));
    }

    int id() {
        return samplerId;
    }

    private int normalizeMinFilter(int filter) {
        return switch (filter) {
            case SamplerData.NEAREST -> GL11.GL_NEAREST;
            case SamplerData.LINEAR -> GL11.GL_LINEAR;
            case SamplerData.NEAREST_MIPMAP_NEAREST -> GL11.GL_NEAREST_MIPMAP_NEAREST;
            case SamplerData.LINEAR_MIPMAP_NEAREST -> GL11.GL_LINEAR_MIPMAP_NEAREST;
            case SamplerData.NEAREST_MIPMAP_LINEAR -> GL11.GL_NEAREST_MIPMAP_LINEAR;
            case SamplerData.LINEAR_MIPMAP_LINEAR -> GL11.GL_LINEAR_MIPMAP_LINEAR;
            default -> GL11.GL_LINEAR;
        };
    }

    private int normalizeMagFilter(int filter) {
        return filter == SamplerData.NEAREST ? GL11.GL_NEAREST : GL11.GL_LINEAR;
    }

    static int normalizeWrap(int wrap) {
        if (wrap == SamplerData.CLAMP_TO_EDGE) {
            return GL12.GL_CLAMP_TO_EDGE;
        }
        if (wrap == SamplerData.MIRRORED_REPEAT) {
            return GL14.GL_MIRRORED_REPEAT;
        }
        return GL11.GL_REPEAT;
    }
}
