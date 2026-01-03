package com.simplerender.gl;

import com.simplerender.asset.TextureData;
import com.simplerender.asset.TextureColorSpace;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;

public final class GpuTexture {
    private final int textureId;

    GpuTexture(TextureData textureData) {
        this(createTextureId(textureData));
    }

    public GpuTexture(int textureId) {
        this.textureId = textureId;
    }

    private static int createTextureId(TextureData textureData) {
        int textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_REPEAT);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        byte[] rgba = textureData.rgba();
        ByteBuffer buffer = BufferUtils.createByteBuffer(rgba.length);
        buffer.put(rgba).flip();
        int internalFormat = textureData.colorSpace() == TextureColorSpace.SRGB
            ? GL21.GL_SRGB8_ALPHA8
            : GL11.GL_RGBA8;
        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            internalFormat,
            textureData.width(),
            textureData.height(),
            0,
            GL11.GL_RGBA,
            GL11.GL_UNSIGNED_BYTE,
            buffer
        );
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        return textureId;
    }

    public int id() {
        return textureId;
    }
}
