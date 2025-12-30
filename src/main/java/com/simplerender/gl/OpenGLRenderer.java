package com.simplerender.gl;

import com.simplerender.asset.MaterialData;
import com.simplerender.asset.MeshData;
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
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
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

    private final String shaderName;

    public OpenGLRenderer(int chunkCount) {
        this(chunkCount, "default");
    }

    public OpenGLRenderer(int chunkCount, String shaderName) {
        this.pipeline = new RenderPipeline(new FrustumCuller());
        this.shaderProgram = new ShaderProgram();
        this.resourceManager = new GpuResourceManager();
        String resolved = shaderName != null && !shaderName.isBlank() ? shaderName : "default";
        this.pendingShaderName = resolved;
        this.activeShaderName = resolved;
        logger.info("Renderer initialized with {} GPU mesh slots", chunkCount);
    }

    public OpenGLRenderer(int chunkCount, String shaderName) {
        this(chunkCount);
        if (shaderName != null && !shaderName.isBlank()) {
            this.pendingShaderName = shaderName;
            this.activeShaderName = shaderName;
        }
    }

    public void render(SceneSnapshot snapshot) {
        ensureInitialized();
        applyPendingShader();
        if (!pipeline.shouldRender(snapshot.camera())) {
            return;
        }
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        shaderProgram.bind();
        uniforms.updateView(snapshot.camera().position(), snapshot.camera().forward(), snapshot.camera().up());
        shaderProgram.setUniformMat4("uProjection", uniforms.projectionMatrix());
        shaderProgram.setUniformMat4("uView", uniforms.viewMatrix());
        shaderProgram.setUniformVec3("uLightDir", uniforms.lightDirection());
        RenderItem[] renderItems = snapshot.renderItems();
        for (int i = 0; i < renderItems.length; i++) {
            RenderItem item = renderItems[i];
            GPUMesh mesh = resourceManager.mesh(item.meshHandle());
            GpuResourceManager.GpuMaterial material = resourceManager.material(item.materialHandle());
            if (mesh == null || material == null) {
                logger.error("Missing GPU resources for render item {}", i);
                continue;
            }
            shaderProgram.setUniformMat4("uModel", item.transform().matrix());
            shaderProgram.setUniformVec3("uBaseColor", material.baseColor());
            GpuTexture texture = resourceManager.texture(material.baseColorTexture());
            bindTextureUnit(GL13.GL_TEXTURE0, texture);
            mesh.draw();
        }
        GLFW.glfwSwapBuffers(window);
    }

    public void pollEvents() {
        if (!initialized) {
            return;
        }
        GLFW.glfwPollEvents();
    }

    public boolean shouldClose() {
        return initialized && GLFW.glfwWindowShouldClose(window);
    }

    public void requestShader(String shaderName) {
        if (shaderName == null || shaderName.isBlank()) {
            return;
        }
        pendingShaderName = shaderName;
    }

    @Override
    public MeshHandle uploadMesh(MeshData meshData) {
        ensureInitialized();
        return resourceManager.uploadMesh(meshData);
    }

    @Override
    public MaterialHandle uploadMaterial(MaterialData materialData) {
        ensureInitialized();
        return resourceManager.uploadMaterial(materialData);
    }

    @Override
    public TextureHandle uploadTexture(TextureData textureData) {
        ensureInitialized();
        return resourceManager.uploadTexture(textureData);
    }

    public void init() {
        if (initialized) {
            return;
        }
        if (!GLFW.glfwInit()) {
            logger.error("Failed to initialize GLFW");
            throw new IllegalStateException("GLFW init failed");
        }
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);
        window = GLFW.glfwCreateWindow(800, 600, "Simple Render", 0, 0);
        if (window == 0) {
            logger.error("Failed to create GLFW window");
            throw new IllegalStateException("Window creation failed");
        }
        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        GLFW.glfwShowWindow(window);
        GL.createCapabilities();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glClearColor(0.12f, 0.12f, 0.12f, 1.0f);
        uniforms = new RenderUniforms(800.0f / 600.0f);
        resourceManager.initDefaultTexture(TextureDataFactory.checkerboard(2, 1));
        ShaderSource shaderSource = ShaderSourceLoader.loadByName(pendingShaderName);
        shaderProgram.init(shaderSource.vertexSource(), shaderSource.fragmentSource());
        shaderProgram.bind();
        shaderProgram.setUniformMat4("uProjection", uniforms.projectionMatrix());
        shaderProgram.setUniformInt("uTexture", 0);
        activeShaderName = pendingShaderName;
        initialized = true;
        logger.info("OpenGL context initialized");
    }

    private void applyPendingShader() {
        if (!shaderProgram.isInitialized()) {
            return;
        }
        if (pendingShaderName.equals(activeShaderName)) {
            return;
        }
        ShaderSource shaderSource = ShaderSourceLoader.loadByName(pendingShaderName);
        shaderProgram.dispose();
        shaderProgram.init(shaderSource.vertexSource(), shaderSource.fragmentSource());
        shaderProgram.bind();
        shaderProgram.setUniformMat4("uProjection", uniforms.projectionMatrix());
        shaderProgram.setUniformInt("uTexture", 0);
        activeShaderName = pendingShaderName;
    }

    private void bindTextureUnit(int textureUnit, GpuTexture texture) {
        GpuTexture resolved = texture != null ? texture : resourceManager.defaultTexture();
        if (resolved == null) {
            return;
        }
        GL13.glActiveTexture(textureUnit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, resolved.id());
    }

    public long windowHandle() {
        ensureInitialized();
        return window;
    }

    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Renderer not initialized");
        }
    }
}
