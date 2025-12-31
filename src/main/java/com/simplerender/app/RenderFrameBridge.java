package com.simplerender.app;

import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

public final class RenderFrameBridge {
    private static final PixelFormat<ByteBuffer> PIXEL_FORMAT = PixelFormat.getByteBgraInstance();

    private final Object lock = new Object();
    private final ImageView imageView;
    private final Deque<ByteBuffer> bufferPool = new ArrayDeque<>();
    private ByteBuffer pendingBuffer;
    private int pendingWidth;
    private int pendingHeight;
    private boolean updateScheduled;
    private WritableImage image;

    public RenderFrameBridge(ImageView imageView) {
        this.imageView = imageView;
    }

    public void submitFrame(ByteBuffer source, int width, int height) {
        if (width <= 0 || height <= 0 || source == null) {
            return;
        }
        int size = width * height * 4;
        ByteBuffer buffer = acquireBuffer(size);
        int stride = width * 4;
        for (int row = 0; row < height; row++) {
            int srcRow = (height - 1 - row) * stride;
            int destRow = row * stride;
            for (int col = 0; col < stride; col++) {
                buffer.put(destRow + col, source.get(srcRow + col));
            }
        }
        buffer.position(0);
        buffer.limit(size);
        synchronized (lock) {
            if (pendingBuffer != null) {
                releaseBuffer(pendingBuffer);
            }
            pendingBuffer = buffer;
            pendingWidth = width;
            pendingHeight = height;
            if (!updateScheduled) {
                updateScheduled = true;
                Platform.runLater(this::applyFrame);
            }
        }
    }

    private void applyFrame() {
        while (true) {
            ByteBuffer buffer;
            int width;
            int height;
            synchronized (lock) {
                buffer = pendingBuffer;
                width = pendingWidth;
                height = pendingHeight;
                pendingBuffer = null;
                if (buffer == null) {
                    updateScheduled = false;
                    return;
                }
            }
            if (image == null || image.getWidth() != width || image.getHeight() != height) {
                image = new WritableImage(width, height);
                imageView.setImage(image);
            }
            PixelWriter writer = image.getPixelWriter();
            writer.setPixels(0, 0, width, height, PIXEL_FORMAT, buffer, width * 4);
            releaseBuffer(buffer);
        }
    }

    private ByteBuffer acquireBuffer(int size) {
        ByteBuffer buffer = bufferPool.pollFirst();
        if (buffer == null || buffer.capacity() < size) {
            return ByteBuffer.allocateDirect(size);
        }
        return buffer;
    }

    private void releaseBuffer(ByteBuffer buffer) {
        buffer.clear();
        bufferPool.addFirst(buffer);
    }
}
