package com.simplerender.render;

import java.nio.ByteBuffer;

/**
 * Receives each finished frame from the {@link Renderer}.
 */
@FunctionalInterface
public interface FrameListener {
    /**
     * Called on the render thread once per frame.
     *
     * @param bgraPixels {@code width * height * 4} bytes in BGRA order, <b>bottom row first</b>
     *                   (OpenGL convention). Only valid during this call: copy what you need.
     */
    void onFrame(ByteBuffer bgraPixels, int width, int height);
}
