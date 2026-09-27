package com.simplerender.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class FrameViewTest {
    @Test
    void copyFlippedPutsBottomRowLast() {
        // 1 pixel wide, 3 rows, OpenGL order: bottom row first.
        ByteBuffer bottomUp = ByteBuffer.wrap(new byte[] { 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3 });
        ByteBuffer topDown = ByteBuffer.allocate(12);
        FrameView.copyFlipped(bottomUp, topDown, 1, 3);
        assertArrayEquals(new byte[] { 3, 3, 3, 3, 2, 2, 2, 2, 1, 1, 1, 1 }, topDown.array());
    }
}
