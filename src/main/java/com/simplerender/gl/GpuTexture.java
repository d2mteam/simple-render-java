package com.simplerender.gl;

import com.simplerender.asset.TextureData;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;

final class GpuTexture {
    private final int textureId;

    GpuTexture(TextureData textureData) {
        textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_REPEAT);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        byte[] rgba = textureData.rgba();
        ByteBuffer buffer = BufferUtils.createByteBuffer(rgba.length);
        buffer.put(rgba).flip();
        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            GL11.GL_RGBA8,
            textureData.width(),
            textureData.height(),
            0,
            GL11.GL_RGBA,
            GL11.GL_UNSIGNED_BYTE,
            buffer
        );
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
    }

    int id() {
        return textureId;
    }
}
