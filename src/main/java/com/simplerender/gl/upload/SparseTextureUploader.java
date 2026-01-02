package com.simplerender.gl.upload;

import com.simplerender.asset.TextureData;
import com.simplerender.gl.GpuTexture;
import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBSparseTexture;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL42;

public final class SparseTextureUploader {
    public GpuTexture upload(TextureData textureData) {
        int textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_REPEAT);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

        if (GL.getCapabilities().GL_ARB_sparse_texture) {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, ARBSparseTexture.GL_TEXTURE_SPARSE_ARB, GL11.GL_TRUE);
        }

        byte[] rgba = textureData.rgba();
        ByteBuffer buffer = BufferUtils.createByteBuffer(rgba.length);
        buffer.put(rgba).flip();
        if (GL.getCapabilities().OpenGL42) {
            GL42.glTexStorage2D(GL11.GL_TEXTURE_2D, 1, GL11.GL_RGBA8, textureData.width(), textureData.height());
            GL11.glTexSubImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                0,
                0,
                textureData.width(),
                textureData.height(),
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                buffer
            );
        } else {
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
        }
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        return new GpuTexture(textureId);
    }
}
