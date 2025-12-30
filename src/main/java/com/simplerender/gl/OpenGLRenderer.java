package com.simplerender.gl;

import com.simplerender.render.culling.FrustumCuller;
import com.simplerender.render.mesh.MeshCache;
import com.simplerender.render.pipeline.RenderPipeline;
import com.simplerender.scene.SceneSnapshot;
import com.simplerender.world.ChunkMeshData;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OpenGLRenderer {
    private static final Logger logger = LoggerFactory.getLogger(OpenGLRenderer.class);

    private final RenderPipeline[] pipelines;
    private final GPUMesh[] gpuMeshes;
    private final ShaderProgram shaderProgram;
    private RenderUniforms uniforms;
    private boolean initialized;
    private long window;

    public OpenGLRenderer(int chunkCount) {
        this.pipelines = new RenderPipeline[chunkCount];
        this.gpuMeshes = new GPUMesh[chunkCount];
        this.shaderProgram = new ShaderProgram();
        for (int i = 0; i < chunkCount; i++) {
            pipelines[i] = new RenderPipeline(new MeshCache(), new FrustumCuller());
            gpuMeshes[i] = new GPUMesh();
        }
        logger.info("Renderer initialized with {} GPU mesh slots", chunkCount);
    }

    public void render(SceneSnapshot snapshot) {
        if (!initialized) {
            initWindow();
        }
        if (pipelines.length == 0) {
            logger.error("Renderer has no GPU mesh slots configured");
            return;
        }
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        shaderProgram.bind();
        uniforms.updateView(snapshot.camera().position(), snapshot.camera().forward(), snapshot.camera().up());
        shaderProgram.setUniformMat4("uProjection", uniforms.projectionMatrix());
        shaderProgram.setUniformMat4("uView", uniforms.viewMatrix());
        shaderProgram.setUniformVec3("uLightDir", uniforms.lightDirection());
        ChunkMeshData[] chunks = snapshot.chunkMeshData();
        int count = Math.min(chunks.length, gpuMeshes.length);
        for (int i = 0; i < count; i++) {
            ChunkMeshData chunk = chunks[i];
            RenderPipeline pipeline = pipelines[i];
            GPUMesh gpuMesh = gpuMeshes[i];
            if (!pipeline.shouldRender(snapshot.camera(), chunk)) {
                continue;
            }
            if (gpuMesh.needsUpload(chunk)) {
                gpuMesh.upload(chunk);
                pipeline.markUploaded(chunk);
                logger.debug("Uploaded chunk {} with {} vertices", i, chunk.vertexCount());
            }
            gpuMesh.draw();
        }
        GLFW.glfwSwapBuffers(window);
        logger.info("Rendered {} chunks", count);
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
        GLFW.glfwShowWindow(window);
        GL.createCapabilities();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glClearColor(0.12f, 0.12f, 0.12f, 1.0f);
        uniforms = new RenderUniforms(800.0f / 600.0f);
        shaderProgram.init(vertexShaderSource(), fragmentShaderSource());
        shaderProgram.bind();
        shaderProgram.setUniformMat4("uProjection", uniforms.projectionMatrix());
        initialized = true;
        logger.info("OpenGL context initialized");
    }

    private String vertexShaderSource() {
        return "#version 330 core\n"
            + "layout(location = 0) in vec3 aPos;\n"
            + "layout(location = 1) in vec3 aNormal;\n"
            + "uniform mat4 uProjection;\n"
            + "uniform mat4 uView;\n"
            + "out vec3 vNormal;\n"
            + "void main() {\n"
            + "    vNormal = aNormal;\n"
            + "    gl_Position = uProjection * uView * vec4(aPos, 1.0);\n"
            + "}\n";
    }

    private String fragmentShaderSource() {
        return "#version 330 core\n"
            + "in vec3 vNormal;\n"
            + "uniform vec3 uLightDir;\n"
            + "out vec4 FragColor;\n"
            + "void main() {\n"
            + "    float diff = max(dot(normalize(vNormal), normalize(-uLightDir)), 0.0);\n"
            + "    vec3 base = vec3(0.2, 0.8, 0.4);\n"
            + "    vec3 color = base * (0.2 + diff * 0.8);\n"
            + "    FragColor = vec4(color, 1.0);\n"
            + "}\n";
    }
}
