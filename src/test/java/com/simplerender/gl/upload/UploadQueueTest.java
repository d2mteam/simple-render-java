package com.simplerender.gl.upload;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class UploadQueueTest {
    @Test
    void testProcessExecutesTasks() {
        UploadQueue queue = new UploadQueue(null, null, null, null, null);
        AtomicBoolean executed = new AtomicBoolean(false);
        queue.enqueue(() -> executed.set(true));
        queue.process();
        assertTrue(executed.get(), "Task should be executed by process()");
    }

    @Test
    void testProcessExecutesMultipleTasks() {
        UploadQueue queue = new UploadQueue(null, null, null, null, null);
        AtomicBoolean exec1 = new AtomicBoolean(false);
        AtomicBoolean exec2 = new AtomicBoolean(false);
        queue.enqueue(() -> exec1.set(true));
        queue.enqueue(() -> exec2.set(true));
        queue.process();
        assertTrue(exec1.get());
        assertTrue(exec2.get());
    }
}
