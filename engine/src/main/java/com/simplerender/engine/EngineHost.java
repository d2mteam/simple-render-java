package com.simplerender.engine;

import com.simplerender.render.FrameListener;
import com.simplerender.scene.InputState;

/**
 * What the engine needs from the program that hosts it (the JavaFX UI, a test, a video
 * recorder, ...). This is the whole contract in the engine-to-host direction.
 *
 * <p>All three methods are called on the engine's render thread. Implementations must be
 * quick and must not block; hand work over to your own thread if needed.
 */
public interface EngineHost extends FrameListener {

    /** The input to apply this frame. Return {@link InputState#IDLE} if there is none. */
    InputState pollInput();

    /** The render loop has ended. {@code error} is {@code null} for a normal stop. */
    default void engineStopped(Throwable error) {
    }
}
