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
import com.simplerender.app.InputState;

public final class OpenGLRenderer implements MeshUploader {
    private static final Logger logger = LoggerFactory.getLogger(OpenGLRenderer.class);

    private final RenderPipeline pipeline;
    private final ShaderProgram shaderProgram;
    private final GpuResourceManager resourceManager;
    private RenderUniforms uniforms;
    private boolean initialized;
    private volatile String pendingShaderName;
    private String activeShaderName;
    private long window;
    private double lastMouseX;
    private double lastMouseY;
    private boolean firstMouse = true;
    private final double[] cursorPosX = new double[1];
    private final double[] cursorPosY = new double[1];

    private final String shaderName;

    public OpenGLRenderer(int chunkCount) {
        this(chunkCount, "default");
    }

    public OpenGLRenderer(int chunkCount, String shaderName) {
        this.pipeline = new RenderPipeline(new FrustumCuller());
        this.shaderProgram = new ShaderProgram();
        this.resourceManager = new GpuResourceManager();
        this.pendingShaderName = "default";
        this.activeShaderName = "default";
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
        if (!initialized) {
            initWindow();
        }
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
            GpuTexture texture = resourceManager.texture(material.textureHandle());
            bindTextureUnit(GL13.GL_TEXTURE0, texture);
            mesh.draw();
        }
        GLFW.glfwSwapBuffers(window);
        logger.info("Rendered {} items", renderItems.length);
    }

    public void pollEvents() {
        if (!initialized) {
            return;
        }
        GLFW.glfwPollEvents();
    }

    public InputState readInput() {
        if (!initialized) {
            return new InputState(false, false, false, false, false, false, 0.0, 0.0);
        }
        boolean forward = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS;
        boolean backward = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS;
        boolean left = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS;
        boolean right = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS;
        boolean up = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;
        boolean down = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS;

        GLFW.glfwGetCursorPos(window, cursorPosX, cursorPosY);
        if (firstMouse) {
            lastMouseX = cursorPosX[0];
            lastMouseY = cursorPosY[0];
            firstMouse = false;
        }
        double deltaX = cursorPosX[0] - lastMouseX;
        double deltaY = cursorPosY[0] - lastMouseY;
        lastMouseX = cursorPosX[0];
        lastMouseY = cursorPosY[0];

        return new InputState(forward, backward, left, right, up, down, deltaX, deltaY);
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
        if (!initialized) {
            initWindow();
        }
        return resourceManager.uploadMesh(meshData);
    }

    @Override
    public MaterialHandle uploadMaterial(MaterialData materialData) {
        if (!initialized) {
            initWindow();
        }
        return resourceManager.uploadMaterial(materialData);
    }

    @Override
    public TextureHandle uploadTexture(TextureData textureData) {
        if (!initialized) {
            initWindow();
        }
        return resourceManager.uploadTexture(textureData);
    }

    private void initWindow() {
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
        if (texture == null) {
            return;
        }
        GL13.glActiveTexture(textureUnit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.id());
    }
}
