package com.simplerender.render.gl;

import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An off-screen image to render into (a framebuffer object): an RGBA8 colour texture and,
 * optionally, a depth texture. Both textures can be sampled by a later pass.
 */
public final class RenderTarget implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(RenderTarget.class);

    private final int width;
    private final int height;
    private final int framebuffer;
    private final int colorTexture;
    private final int depthTexture; // 0 when the target has no depth

    private RenderTarget(int width, int height, boolean withDepth) {
        this.width = width;
        this.height = height;
        framebuffer = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);

        colorTexture = createTexture(GL11.GL_RGBA8, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D,
                colorTexture, 0);
        if (withDepth) {
            depthTexture = createTexture(GL30.GL_DEPTH24_STENCIL8, GL30.GL_DEPTH_STENCIL,
                    GL30.GL_UNSIGNED_INT_24_8);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL11.GL_TEXTURE_2D,
                    depthTexture, 0);
        } else {
            depthTexture = 0;
        }

        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            logger.error("Framebuffer {}x{} is incomplete (status 0x{})", width, height, Integer.toHexString(status));
        }
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    /** Colour + depth, for drawing 3D geometry. */
    public static RenderTarget withDepth(int width, int height) {
        return new RenderTarget(width, height, true);
    }

    /** Colour only, for full-screen passes. */
    public static RenderTarget colorOnly(int width, int height) {
        return new RenderTarget(width, height, false);
    }

    /** Makes this the target of draw calls and sets the viewport to cover it. */
    public void bind() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        GL11.glViewport(0, 0, width, height);
    }

    public void bindColorTexture(int unit) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorTexture);
    }

    public void bindDepthTexture(int unit) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTexture);
    }

    /**
     * Copies the colour image into {@code out} as BGRA bytes, bottom row first.
     * {@code out} must hold {@code width * height * 4} bytes.
     */
    public void readPixelsBgra(ByteBuffer out) {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glReadPixels(0, 0, width, height, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, out);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public float aspectRatio() {
        return (float) width / (float) height;
    }

    @Override
    public void close() {
        GL30.glDeleteFramebuffers(framebuffer);
        GL11.glDeleteTextures(colorTexture);
        if (depthTexture != 0) {
            GL11.glDeleteTextures(depthTexture);
        }
    }

    private int createTexture(int internalFormat, int format, int type) {
        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, (ByteBuffer) null);
        // Linear filtering also for depth: the post-process effects read depth between texels.
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        return texture;
    }
}
