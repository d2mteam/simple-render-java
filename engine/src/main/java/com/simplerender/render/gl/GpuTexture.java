package com.simplerender.render.gl;

import com.simplerender.asset.TextureColorSpace;
import com.simplerender.asset.TextureData;
import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;

/**
 * A 2D RGBA texture with mipmaps.
 *
 * <p>sRGB textures (colour data such as base colour) are stored as {@code GL_SRGB8_ALPHA8}
 * so the GPU converts them to linear values when sampling; everything else is plain RGBA8.
 */
public final class GpuTexture implements AutoCloseable {
    private final int textureId;

    public GpuTexture(TextureData texture) {
        textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);

        int internalFormat = texture.colorSpace() == TextureColorSpace.SRGB ? GL21.GL_SRGB8_ALPHA8 : GL11.GL_RGBA8;
        byte[] rgba = texture.rgba();
        ByteBuffer pixels = BufferUtils.createByteBuffer(rgba.length).put(rgba).flip();
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, texture.width(), texture.height(), 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
    }

    public void bind(int unit) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
    }

    @Override
    public void close() {
        GL11.glDeleteTextures(textureId);
    }
}
