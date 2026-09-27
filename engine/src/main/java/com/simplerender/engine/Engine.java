package com.simplerender.engine;

import com.simplerender.asset.plugin.ModelImportService;
import com.simplerender.asset.plugin.ModelImporter;
import com.simplerender.render.PostProcessSettings;
import com.simplerender.render.Renderer;
import com.simplerender.render.ShaderLibrary;
import com.simplerender.scene.Scene;
import com.simplerender.scene.SceneObject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The engine's public face: owns the scene, the model importers and the render thread.
 *
 * <p><b>Threading model.</b> {@link #start} launches one render thread. That thread creates
 * the {@link Renderer} (and with it the OpenGL context) and runs the loop
 * <pre>
 *   apply requests ─► scene.update(input) ─► renderer.render(snapshot) ─► host.onFrame(pixels)
 * </pre>
 * Every other public method may be called from any thread:
 * <ul>
 *   <li>{@code request...} methods only record the latest wish; the render thread picks it
 *       up at the start of the next frame. No GL call ever happens on the caller's thread.</li>
 *   <li>{@link #scene()} is thread-safe, and {@link #loadModel} runs on the caller's thread
 *       (it only reads files), so call it from a background thread.</li>
 * </ul>
 */
public final class Engine {
    private static final Logger logger = LoggerFactory.getLogger(Engine.class);

    private record Size(int width, int height) {
    }

    private final EngineConfig config;
    private final Scene scene = new Scene();
    private final ModelImportService importService;

    private final AtomicReference<String> requestedShader = new AtomicReference<>();
    private final AtomicReference<Size> requestedSize = new AtomicReference<>();
    private final AtomicReference<PostProcessSettings> requestedPostProcess = new AtomicReference<>();

    private volatile boolean running;
    private Thread renderThread;

    /** Creates the engine and loads the model importer plugins from {@code plugins/}. */
    public Engine(EngineConfig config) {
        this(config, ModelImportService.createDefault());
    }

    /** Creates the engine and loads the plugins of {@code importService}. Does not start rendering. */
    public Engine(EngineConfig config, ModelImportService importService) {
        this.config = config;
        this.importService = importService;
        importService.loadPlugins();
    }

    // ---- Lifecycle --------------------------------------------------------------------------

    /** Starts the render thread. Frames and the stop notification go to {@code host}. */
    public synchronized void start(EngineHost host) {
        if (renderThread != null) {
            throw new IllegalStateException("Engine already started");
        }
        running = true;
        renderThread = new Thread(() -> runLoop(host), "render-thread");
        renderThread.start();
    }

    /** Asks the render loop to finish and waits (up to 5 s) for it to release the GPU. */
    public void stop() {
        running = false;
        Thread thread;
        synchronized (this) {
            thread = renderThread;
        }
        if (thread != null && thread != Thread.currentThread()) {
            try {
                thread.join(TimeUnit.SECONDS.toMillis(5));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ---- Scene and assets -------------------------------------------------------------------

    public Scene scene() {
        return scene;
    }

    /**
     * Imports a model file and adds its parts to the scene. Blocks while the file is read,
     * so call it off the UI thread.
     *
     * @return the names of the objects that were added
     */
    public List<String> loadModel(Path path) {
        ModelImporter.ImportedModel model = importService.importModel(path);
        String fileName = path.getFileName().toString();
        List<SceneObject> objects = new ArrayList<>();
        for (ModelImporter.ImportedPrimitive primitive : model.primitives()) {
            String name = model.primitives().size() == 1 ? fileName : fileName + " #" + objects.size();
            objects.add(new SceneObject(name, primitive.meshData(), primitive.materialData(), primitive.transform()));
        }
        scene.addAll(objects);
        return objects.stream().map(SceneObject::name).toList();
    }

    public List<String> supportedModelExtensions() {
        return importService.supportedExtensions();
    }

    public List<String> sceneShaders() {
        return ShaderLibrary.SCENE_SHADERS;
    }

    // ---- Requests from other threads --------------------------------------------------------

    public void requestShader(String shaderName) {
        requestedShader.set(shaderName);
    }

    public void requestResize(int width, int height) {
        requestedSize.set(new Size(width, height));
    }

    /** The engine stores a copy, so the caller may keep editing {@code settings}. */
    public void requestPostProcessSettings(PostProcessSettings settings) {
        requestedPostProcess.set(settings.copy());
    }

    // ---- Render thread ----------------------------------------------------------------------

    private void runLoop(EngineHost host) {
        Throwable failure = null;
        try (Renderer renderer = new Renderer(config.shaderName())) {
            long frameBudgetNanos = config.targetFps() > 0 ? TimeUnit.SECONDS.toNanos(1) / config.targetFps() : 0;
            long previousFrameStart = System.nanoTime();
            int frameCount = 0;
            logger.info("Render loop started");
            while (running) {
                long frameStart = System.nanoTime();
                float deltaSeconds = (frameStart - previousFrameStart) / 1_000_000_000.0f;
                previousFrameStart = frameStart;

                applyRequests(renderer);
                scene.update(deltaSeconds, host.pollInput());
                renderer.render(scene.snapshot(), host);

                frameCount++;
                if (config.maxFrames() > 0 && frameCount >= config.maxFrames()) {
                    logger.info("Rendered {} frames, stopping", frameCount);
                    break;
                }
                sleepUntil(frameStart + frameBudgetNanos);
            }
        } catch (Throwable t) {
            failure = t;
            logger.error("Render loop failed", t);
        } finally {
            running = false;
            logger.info("Render loop stopped");
            host.engineStopped(failure);
        }
    }

    private void applyRequests(Renderer renderer) {
        Size size = requestedSize.getAndSet(null);
        if (size != null) {
            renderer.resize(size.width(), size.height());
        }
        String shader = requestedShader.getAndSet(null);
        if (shader != null) {
            renderer.setSceneShader(shader);
        }
        PostProcessSettings postProcess = requestedPostProcess.getAndSet(null);
        if (postProcess != null) {
            renderer.setPostProcessSettings(postProcess);
        }
    }

    private void sleepUntil(long deadlineNanos) {
        long remaining = deadlineNanos - System.nanoTime();
        if (remaining <= 0) {
            return;
        }
        try {
            TimeUnit.NANOSECONDS.sleep(remaining);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }
}
