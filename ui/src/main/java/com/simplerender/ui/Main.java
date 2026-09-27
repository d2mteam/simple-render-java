package com.simplerender.ui;

import javafx.application.Application;

/**
 * Program entry point.
 *
 * <p>Options (both {@code --key value} and {@code --key=value} work):
 * <pre>
 *   --model &lt;file&gt;      model to load at start-up (.obj, .gltf, .glb)
 *   --shader &lt;name&gt;     default | disney_brdf | debug_mesh
 *   --max-frames &lt;n&gt;   quit after n frames (handy for smoke tests)
 * </pre>
 *
 * <p>This class deliberately does not extend {@link Application}: launching through a
 * plain main class avoids the "JavaFX runtime components are missing" check when JavaFX
 * is on the class path instead of the module path.
 */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        Application.launch(SimpleRenderApp.class, args);
    }
}
