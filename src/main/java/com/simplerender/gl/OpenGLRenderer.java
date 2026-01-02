package com.simplerender.gl;

import com.simplerender.asset.MaterialData;
import com.simplerender.asset.MeshData;
import com.simplerender.asset.SamplerData;
import com.simplerender.asset.ShaderSource;
import com.simplerender.asset.ShaderSourceLoader;
import com.simplerender.asset.TextureData;
import com.simplerender.asset.TextureDataFactory;
import com.simplerender.render.MaterialHandle;
import com.simplerender.render.MeshHandle;
import com.simplerender.render.MeshUploader;
import com.simplerender.render.RenderItem;
import com.simplerender.render.TextureHandle;
import com.simplerender.render.culling.FrustumCuller;
import com.simplerender.render.pipeline.RenderPipeline;
import com.simplerender.scene.SceneSnapshot;
import com.simplerender.app.RenderFrameBridge;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OpenGLRenderer implements MeshUploader {
    private static final Logger logger = LoggerFactory.getLogger(OpenGLRenderer.class);

    private final RenderPipeline pipeline;
    private final ShaderProgram shaderProgram;
    private final GpuResourceManager resourceManager;
    private RenderUniforms uniforms;
    private boolean initialized;
    private String pendingShaderName;
    private String activeShaderName;
    private long window;
    private final Queue<Runnable> pendingTasks = new ConcurrentLinkedQueue<>();
    private Thread renderThread;
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private RenderFrameBridge frameBridge;
    private int framebuffer;
    private int colorTexture;
    private int depthBuffer;
    private int renderWidth = 1;
    private int renderHeight = 1;
    private ByteBuffer pixelBuffer;

    public OpenGLRenderer() {
        this("default");
    }

    public OpenGLRenderer(String shaderName) {
        this.pipeline = new RenderPipeline(new FrustumCuller());
        this.shaderProgram = new ShaderProgram();
        this.resourceManager = new GpuResourceManager();
        String resolved = shaderName != null && !shaderName.isBlank() ? shaderName : "default";
        this.pendingShaderName = resolved;
        this.activeShaderName = resolved;
        logger.info("Renderer initialized");
    }

    public void render(SceneSnapshot snapshot) {
        ensureInitialized();
        drainPendingTasks();
        applyPendingShader();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        GL11.glViewport(0, 0, renderWidth, renderHeight);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        shaderProgram.bind();
        uniforms.updateView(snapshot.camera().position(), snapshot.camera().forward(), snapshot.camera().up());
        shaderProgram.setUniformMat4("uProjection", uniforms.projectionMatrix());
        shaderProgram.setUniformMat4("uView", uniforms.viewMatrix());
        applyLightUniforms();
        pipeline.updateFrustum(uniforms.projectionMatrix(), uniforms.viewMatrix());
        RenderItem[] renderItems = snapshot.renderItems();
        for (int i = 0; i < renderItems.length; i++) {
            RenderItem item = renderItems[i];
            GPUMesh mesh = resourceManager.mesh(item.meshHandle());
            GpuResourceManager.GpuMaterial material = resourceManager.material(item.materialHandle());
            if (mesh == null || material == null) {
                logger.error("Missing GPU resources for render item {}", i);
                continue;
            }
            if (!pipeline.shouldRender(item, mesh.snapshot())) {
                continue;
            }
            shaderProgram.setUniformMat4("uModel", item.transform().matrix());
            shaderProgram.setUniformVec3("uBaseColor", material.baseColor());
            bindTextureUnit(
                GL13.GL_TEXTURE0,
                resourceManager.texture(material.baseColorTexture()),
                resourceManager.sampler(material.baseColorSampler())
            );
            bindTextureUnit(
                GL13.GL_TEXTURE1,
                resourceManager.texture(material.normalTexture()),
                resourceManager.sampler(material.normalSampler())
            );
            bindTextureUnit(
                GL13.GL_TEXTURE2,
                resourceManager.texture(material.metallicRoughnessTexture()),
                resourceManager.sampler(material.metallicRoughnessSampler())
            );
            bindTextureUnit(
                GL13.GL_TEXTURE3,
                resourceManager.texture(material.aoTexture()),
                resourceManager.sampler(material.aoSampler())
            );
            bindTextureUnit(
                GL13.GL_TEXTURE4,
                resourceManager.texture(material.emissiveTexture()),
                resourceManager.sampler(material.emissiveSampler())
            );
            mesh.draw();
        }
        if (frameBridge != null) {
            ensurePixelBuffer();
            GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            GL11.glReadPixels(0, 0, renderWidth, renderHeight, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, pixelBuffer);
            pixelBuffer.rewind();
            frameBridge.submitFrame(pixelBuffer, renderWidth, renderHeight);
        }
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glFlush();
    }

    public boolean shouldClose() {
        return stopRequested.get();
    }

    public void requestShader(String shaderName) {
        if (shaderName == null || shaderName.isBlank()) {
            return;
        }
        if (Thread.currentThread() == renderThread) {
            pendingShaderName = shaderName;
        } else {
            submit(() -> pendingShaderName = shaderName);
        }
    }

    @Override
    public MeshHandle uploadMesh(MeshData meshData) {
        ensureInitialized();
        if (Thread.currentThread() == renderThread) {
            return resourceManager.uploadMesh(meshData);
        }
        return submit(() -> resourceManager.uploadMesh(meshData)).join();
    }

    @Override
    public MaterialHandle uploadMaterial(MaterialData materialData) {
        ensureInitialized();
        if (Thread.currentThread() == renderThread) {
            return resourceManager.uploadMaterial(materialData);
        }
        return submit(() -> resourceManager.uploadMaterial(materialData)).join();
    }

    @Override
    public TextureHandle uploadTexture(TextureData textureData) {
        ensureInitialized();
        if (Thread.currentThread() == renderThread) {
            return resourceManager.uploadTexture(textureData);
        }
        return submit(() -> resourceManager.uploadTexture(textureData)).join();
    }

    public void init() {
        if (initialized) {
            return;
        }
        renderThread = Thread.currentThread();
        if (!GLFW.glfwInit()) {
            logger.error("Failed to initialize GLFW");
            throw new IllegalStateException("GLFW init failed");
        }
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);
        window = GLFW.glfwCreateWindow(1, 1, "Simple Render", 0, 0);
        if (window == 0) {
            logger.error("Failed to create GLFW window");
            throw new IllegalStateException("Window creation failed");
        }
        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glClearColor(0.12f, 0.12f, 0.12f, 1.0f);
        uniforms = new RenderUniforms(1.0f);
        resourceManager.initDefaultTextures(
            TextureDataFactory.solidColor(255, 255, 255, 255),
            TextureDataFactory.solidColor(128, 128, 255, 255),
            TextureDataFactory.solidColor(0, 255, 0, 255),
            TextureDataFactory.solidColor(255, 255, 255, 255),
            TextureDataFactory.solidColor(0, 0, 0, 255)
        );
        resourceManager.initDefaultSampler(SamplerData.defaults());
        ShaderSource shaderSource = ShaderSourceLoader.loadByName(pendingShaderName);
        shaderProgram.init(shaderSource.vertexSource(), shaderSource.fragmentSource());
        shaderProgram.bind();
        shaderProgram.setUniformMat4("uProjection", uniforms.projectionMatrix());
        bindSamplers();
        activeShaderName = pendingShaderName;
        resizeRenderTarget(renderWidth, renderHeight);
        initialized = true;
        logger.info("OpenGL context initialized");
    }

    public void requestResize(int width, int height) {
        ensureInitialized();
        if (Thread.currentThread() != renderThread) {
            submit(() -> resizeRenderTarget(width, height));
            return;
        }
        resizeRenderTarget(width, height);
    }

    private void resizeRenderTarget(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (width == renderWidth && height == renderHeight) {
            return;
        }
        renderWidth = width;
        renderHeight = height;
        disposeRenderTarget();
        framebuffer = GL30.glGenFramebuffers();
        colorTexture = GL11.glGenTextures();
        depthBuffer = GL30.glGenRenderbuffers();

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorTexture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, colorTexture, 0);

        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthBuffer);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH24_STENCIL8, width, height);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL30.GL_RENDERBUFFER, depthBuffer);

        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            logger.error("Framebuffer incomplete with status {}", status);
        }
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glViewport(0, 0, width, height);
        uniforms.updateProjection((float) width / (float) height);
        shaderProgram.bind();
        shaderProgram.setUniformMat4("uProjection", uniforms.projectionMatrix());
    }

    private void applyPendingShader() {
        if (!shaderProgram.isInitialized()) {
            return;
        }
        if (pendingShaderName.equals(activeShaderName)) {
            return;
        }
        logger.info("Switching shader from {} to {}", activeShaderName, pendingShaderName);
        ShaderSource shaderSource = ShaderSourceLoader.loadByName(pendingShaderName);
        shaderProgram.dispose();
        shaderProgram.init(shaderSource.vertexSource(), shaderSource.fragmentSource());
        shaderProgram.bind();
        shaderProgram.setUniformMat4("uProjection", uniforms.projectionMatrix());
        bindSamplers();
        activeShaderName = pendingShaderName;
    }

    private void applyLightUniforms() {
        int count = uniforms.lightCount();
        shaderProgram.setUniformInt("uLightCount", count);
        for (int i = 0; i < count; i++) {
            RenderUniforms.Light light = uniforms.light(i);
            shaderProgram.setUniformInt("uLightType[" + i + "]", light.type());
            shaderProgram.setUniformVec3("uLightColor[" + i + "]", light.color());
            shaderProgram.setUniformVec3("uLightPosition[" + i + "]", light.position());
            shaderProgram.setUniformVec3("uLightDirection[" + i + "]", light.direction());
            shaderProgram.setUniformVec4("uLightParams[" + i + "]", light.params());
        }
    }

    private void bindSamplers() {
        shaderProgram.setUniformIntIfPresent("uTexture", 0);
        shaderProgram.setUniformIntIfPresent("uBaseColorTex", 0);
        shaderProgram.setUniformIntIfPresent("uNormalTex", 1);
        shaderProgram.setUniformIntIfPresent("uMetallicRoughnessTex", 2);
        shaderProgram.setUniformIntIfPresent("uAoTex", 3);
        shaderProgram.setUniformIntIfPresent("uEmissiveTex", 4);
    }

    private void bindTextureUnit(int textureUnit, GpuTexture texture, GpuSampler sampler) {
        GpuTexture resolved = texture != null ? texture : resourceManager.defaultTexture();
        if (resolved == null) {
            return;
        }
        GpuSampler resolvedSampler = sampler != null ? sampler : resourceManager.defaultSampler();
        GL13.glActiveTexture(textureUnit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, resolved.id());
        if (resolvedSampler != null) {
            GL33.glBindSampler(textureUnit - GL13.GL_TEXTURE0, resolvedSampler.id());
        }
    }

    public long windowHandle() {
        ensureInitialized();
        return window;
    }

    public void requestStop() {
        stopRequested.set(true);
    }

    public void setFrameBridge(RenderFrameBridge frameBridge) {
        if (Thread.currentThread() == renderThread) {
            this.frameBridge = frameBridge;
        } else {
            submit(() -> this.frameBridge = frameBridge);
        }
    }


    public <T> CompletableFuture<T> submit(Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Runnable wrapped = () -> {
            try {
                future.complete(task.call());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        };
        if (Thread.currentThread() == renderThread) {
            wrapped.run();
        } else {
            pendingTasks.add(wrapped);
        }
        return future;
    }

    public CompletableFuture<Void> submit(Runnable task) {
        return submit(() -> {
            task.run();
            return null;
        });
    }

    private void drainPendingTasks() {
        Runnable task;
        while ((task = pendingTasks.poll()) != null) {
            task.run();
        }
    }

    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Renderer not initialized");
        }
    }

    private void ensurePixelBuffer() {
        int size = renderWidth * renderHeight * 4;
        if (pixelBuffer == null || pixelBuffer.capacity() < size) {
            pixelBuffer = BufferUtils.createByteBuffer(size);
        } else {
            pixelBuffer.clear();
        }
    }

    private void disposeRenderTarget() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, 0);
        if (framebuffer != 0) {
            GL30.glDeleteFramebuffers(framebuffer);
            framebuffer = 0;
        }
        if (colorTexture != 0) {
            GL11.glDeleteTextures(colorTexture);
            colorTexture = 0;
        }
        if (depthBuffer != 0) {
            GL30.glDeleteRenderbuffers(depthBuffer);
            depthBuffer = 0;
        }
    }
}
