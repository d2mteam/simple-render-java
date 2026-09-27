package com.simplerender.ui;

import com.simplerender.render.FrameListener;
import java.nio.ByteBuffer;
import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * Shows the engine's frames in the JavaFX window.
 *
 * <p>{@link #onFrame} runs on the render thread: it copies the pixels (flipping them
 * upright) and asks the JavaFX thread to display them. If the JavaFX thread falls behind,
 * older frames are simply dropped, so the engine never waits for the UI. Two pixel buffers
 * are recycled to avoid allocating one per frame.
 */
final class FrameView implements FrameListener {
    private record Frame(ByteBuffer pixels, int width, int height) {
    }

    private final ImageView imageView = new ImageView();
    private final StackPane root = new StackPane(imageView);
    private final Object lock = new Object();
    private Frame pending;             // newest frame not yet shown   (guarded by lock)
    private Frame spare;               // finished buffer to reuse      (guarded by lock)
    private boolean presentScheduled;  // a present() call is queued    (guarded by lock)
    private WritableImage image;       // JavaFX thread only

    FrameView() {
        imageView.setPreserveRatio(false);
        imageView.setSmooth(false);
        // Unmanaged: the image follows the pane's size but never pushes the pane to grow.
        imageView.setManaged(false);
        imageView.fitWidthProperty().bind(root.widthProperty());
        imageView.fitHeightProperty().bind(root.heightProperty());
        root.setMinSize(320, 240);
        root.setPrefSize(960, 600);
        root.setStyle("-fx-background-color: #1f1f1f;");
    }

    Region node() {
        return root;
    }

    @Override
    public void onFrame(ByteBuffer bgraPixels, int width, int height) {
        Frame frame = obtainBuffer(width, height);
        copyFlipped(bgraPixels, frame.pixels(), width, height);
        boolean schedule;
        synchronized (lock) {
            if (pending != null) {
                spare = pending; // the UI never saw it; recycle the buffer
            }
            pending = frame;
            schedule = !presentScheduled;
            presentScheduled = true;
        }
        if (schedule) {
            Platform.runLater(this::present);
        }
    }

    /** Copies rows so the first row of {@code dst} is the top of the image. */
    static void copyFlipped(ByteBuffer bottomUpSrc, ByteBuffer dst, int width, int height) {
        int stride = width * 4;
        for (int row = 0; row < height; row++) {
            dst.put(row * stride, bottomUpSrc, (height - 1 - row) * stride, stride);
        }
    }

    private Frame obtainBuffer(int width, int height) {
        ByteBuffer buffer;
        synchronized (lock) {
            buffer = spare == null ? null : spare.pixels();
            spare = null;
        }
        int size = width * height * 4;
        if (buffer == null || buffer.capacity() < size) {
            buffer = ByteBuffer.allocateDirect(size);
        }
        buffer.clear().limit(size);
        return new Frame(buffer, width, height);
    }

    private void present() {
        Frame frame;
        synchronized (lock) {
            frame = pending;
            pending = null;
            presentScheduled = false;
        }
        if (frame == null) {
            return;
        }
        if (image == null || image.getWidth() != frame.width() || image.getHeight() != frame.height()) {
            image = new WritableImage(frame.width(), frame.height());
            imageView.setImage(image);
        }
        image.getPixelWriter().setPixels(0, 0, frame.width(), frame.height(),
                PixelFormat.getByteBgraInstance(), frame.pixels(), frame.width() * 4);
        synchronized (lock) {
            if (spare == null) {
                spare = frame;
            }
        }
    }
}
