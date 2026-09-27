package com.simplerender.render;

import com.simplerender.render.gl.GlContext;
import com.simplerender.render.gl.RenderTarget;
import com.simplerender.scene.SceneSnapshot;
import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Turns a {@link SceneSnapshot} into pixels.
 *
 * <p>Each frame runs two passes and then reads the result back:
 * <pre>
 *   ScenePass        SceneSnapshot ──► sceneTarget (colour + depth)
 *   PostProcessPass  sceneTarget   ──► finalTarget (colour)
 *   read back        finalTarget   ──► FrameListener (BGRA bytes)
 * </pre>
 *
 * <p><b>Threading:</b> the constructor creates an OpenGL context that is bound to the
 * calling thread. Every method must be called from that same thread; the class does no
 * locking of its own. {@code com.simplerender.engine.Engine} owns that thread.
 */
public final class Renderer implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(Renderer.class);

    private final GlContext context;
    private final GpuResourceCache resources;
    private final ScenePass scenePass;
    private final PostProcessPass postProcessPass;
    private RenderTarget sceneTarget;
    private RenderTarget finalTarget;
    private PostProcessSettings postProcessSettings = new PostProcessSettings();
    private ByteBuffer readbackBuffer;

    public Renderer(String sceneShader) {
        context = GlContext.createOffscreen();
        try {
            resources = new GpuResourceCache();
            scenePass = new ScenePass(resources, sceneShader);
            postProcessPass = new PostProcessPass();
            createTargets(1, 1);
        } catch (RuntimeException e) {
            context.close(); // e.g. a shader failed to compile
            throw e;
        }
    }

    /** Draws one frame and hands the finished image to {@code output}. */
    public void render(SceneSnapshot scene, FrameListener output) {
        scenePass.render(scene, sceneTarget);
        postProcessPass.render(sceneTarget, finalTarget, postProcessSettings);
        readBack(output);
    }

    /** Changes the output size in pixels. Cheap to call with an unchanged size. */
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0 || (width == finalTarget.width() && height == finalTarget.height())) {
            return;
        }
        sceneTarget.close();
        finalTarget.close();
        createTargets(width, height);
        logger.debug("Render size is now {}x{}", width, height);
    }

    /** Switches the scene shader (one of {@link ShaderLibrary#SCENE_SHADERS}). */
    public void setSceneShader(String name) {
        scenePass.setShader(name);
    }

    /** The renderer keeps a reference; pass a copy if you intend to keep editing it. */
    public void setPostProcessSettings(PostProcessSettings settings) {
        this.postProcessSettings = settings;
    }

    @Override
    public void close() {
        scenePass.close();
        postProcessPass.close();
        sceneTarget.close();
        finalTarget.close();
        resources.close();
        context.close();
    }

    private void createTargets(int width, int height) {
        sceneTarget = RenderTarget.withDepth(width, height);
        finalTarget = RenderTarget.colorOnly(width, height);
    }

    private void readBack(FrameListener output) {
        int size = finalTarget.width() * finalTarget.height() * 4;
        if (readbackBuffer == null || readbackBuffer.capacity() < size) {
            readbackBuffer = BufferUtils.createByteBuffer(size);
        }
        readbackBuffer.clear().limit(size);
        finalTarget.readPixelsBgra(readbackBuffer);
        output.onFrame(readbackBuffer, finalTarget.width(), finalTarget.height());
    }
}
