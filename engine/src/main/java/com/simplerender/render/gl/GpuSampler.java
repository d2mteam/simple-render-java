package com.simplerender.render.gl;

import com.simplerender.asset.SamplerData;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL33;

/**
 * An OpenGL sampler object. While bound to a texture unit it overrides the filtering and
 * wrapping of whatever texture is bound there.
 */
public final class GpuSampler implements AutoCloseable {
    private final int samplerId;

    public GpuSampler(SamplerData sampler) {
        samplerId = GL33.glGenSamplers();
        GL33.glSamplerParameteri(samplerId, GL11.GL_TEXTURE_MIN_FILTER, toGlMinFilter(sampler.minFilter()));
        GL33.glSamplerParameteri(samplerId, GL11.GL_TEXTURE_MAG_FILTER, toGlMagFilter(sampler.magFilter()));
        GL33.glSamplerParameteri(samplerId, GL11.GL_TEXTURE_WRAP_S, toGlWrap(sampler.wrapS()));
        GL33.glSamplerParameteri(samplerId, GL11.GL_TEXTURE_WRAP_T, toGlWrap(sampler.wrapT()));
    }

    public void bind(int unit) {
        GL33.glBindSampler(unit, samplerId);
    }

    /** Removes any sampler from the unit, so the texture's own parameters apply again. */
    public static void unbind(int unit) {
        GL33.glBindSampler(unit, 0);
    }

    @Override
    public void close() {
        GL33.glDeleteSamplers(samplerId);
    }

    static int toGlMinFilter(int filter) {
        return switch (filter) {
            case SamplerData.NEAREST -> GL11.GL_NEAREST;
            case SamplerData.NEAREST_MIPMAP_NEAREST -> GL11.GL_NEAREST_MIPMAP_NEAREST;
            case SamplerData.LINEAR_MIPMAP_NEAREST -> GL11.GL_LINEAR_MIPMAP_NEAREST;
            case SamplerData.NEAREST_MIPMAP_LINEAR -> GL11.GL_NEAREST_MIPMAP_LINEAR;
            case SamplerData.LINEAR_MIPMAP_LINEAR -> GL11.GL_LINEAR_MIPMAP_LINEAR;
            default -> GL11.GL_LINEAR;
        };
    }

    static int toGlMagFilter(int filter) {
        return filter == SamplerData.NEAREST ? GL11.GL_NEAREST : GL11.GL_LINEAR;
    }

    static int toGlWrap(int wrap) {
        return switch (wrap) {
            case SamplerData.CLAMP_TO_EDGE -> GL12.GL_CLAMP_TO_EDGE;
            case SamplerData.MIRRORED_REPEAT -> GL14.GL_MIRRORED_REPEAT;
            default -> GL11.GL_REPEAT;
        };
    }
}
