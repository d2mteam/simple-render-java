package com.simplerender.render;

import com.simplerender.asset.MaterialData;
import com.simplerender.math.Matrix4f;
import com.simplerender.render.GpuResourceCache.GpuMaterial;
import com.simplerender.render.GpuResourceCache.TextureBinding;
import com.simplerender.render.gl.GpuSampler;
import com.simplerender.render.gl.RenderTarget;
import com.simplerender.render.gl.ShaderProgram;
import com.simplerender.scene.CameraSnapshot;
import com.simplerender.scene.Light;
import com.simplerender.scene.SceneSnapshot;
import com.simplerender.scene.SceneSnapshot.RenderItem;
import java.util.List;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pass 1: draws every visible object with lighting into a colour + depth target.
 *
 * <p>Uses one of the {@link ShaderLibrary#SCENE_SHADERS}; they all receive the same uniforms:
 * camera matrices, up to {@value #MAX_LIGHTS} lights, the model matrix and the material.
 */
final class ScenePass implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(ScenePass.class);
    private static final int MAX_LIGHTS = 8;
    private static final float[] CLEAR_COLOR = { 0.12f, 0.12f, 0.12f, 1.0f };

    private final GpuResourceCache resources;
    private final FrustumCuller culler = new FrustumCuller();
    private ShaderProgram shader;
    private String shaderName;

    ScenePass(GpuResourceCache resources, String shaderName) {
        this.resources = resources;
        setShader(shaderName);
    }

    /**
     * Switches to another scene shader; does nothing if it is already active. If the new
     * shader does not compile, the current one stays in use (the very first one must compile).
     */
    void setShader(String name) {
        String resolved = ShaderLibrary.resolveSceneShader(name);
        if (resolved.equals(shaderName)) {
            return;
        }
        ShaderProgram next;
        try {
            next = ShaderLibrary.loadSceneShader(resolved);
        } catch (RuntimeException e) {
            if (shader == null) {
                throw e;
            }
            logger.error("Could not switch to shader '{}', keeping '{}'", resolved, shaderName, e);
            return;
        }
        if (shader != null) {
            shader.close();
        }
        shader = next;
        shaderName = resolved;
        shader.use();
        for (MaterialTexture slot : MaterialTexture.values()) {
            shader.setInt(slot.samplerUniform, slot.unit());
        }
    }

    void render(SceneSnapshot scene, RenderTarget target) {
        target.bind();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true); // must be on for glClear to clear depth
        GL11.glClearColor(CLEAR_COLOR[0], CLEAR_COLOR[1], CLEAR_COLOR[2], CLEAR_COLOR[3]);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        CameraSnapshot camera = scene.camera();
        float[] view = Matrix4f.lookAt(camera.position(), camera.position().add(camera.forward()), camera.up());
        float[] projection = Matrix4f.perspective(
                (float) Math.toRadians(camera.fovDegrees()), target.aspectRatio(), camera.nearPlane(),
                camera.farPlane());
        culler.update(projection, view);

        shader.use();
        shader.setMat4("uProjection", projection);
        shader.setMat4("uView", view);
        shader.setVec3("uCameraPos", new float[] { camera.position().x(), camera.position().y(), camera.position().z() });
        setLights(scene.lights());

        for (RenderItem item : scene.items()) {
            if (!culler.isVisible(item.mesh(), item.modelMatrix())) {
                continue;
            }
            shader.setMat4("uModel", item.modelMatrix());
            setMaterial(resources.material(item.material()));
            resources.mesh(item.mesh()).draw();
        }

        // Leave clean state for the next pass: no blending, depth writes on, no sampler overrides.
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
        for (MaterialTexture slot : MaterialTexture.values()) {
            GpuSampler.unbind(slot.unit());
        }
    }

    private void setLights(List<Light> lights) {
        int count = Math.min(lights.size(), MAX_LIGHTS);
        shader.setInt("uLightCount", count);
        for (int i = 0; i < count; i++) {
            Light light = lights.get(i);
            shader.setInt("uLightType[" + i + "]", light.type().ordinal());
            shader.setVec3("uLightColor[" + i + "]", light.color());
            shader.setVec3("uLightPosition[" + i + "]", light.position());
            shader.setVec3("uLightDirection[" + i + "]", light.direction());
            // Packed as the shaders expect: x = intensity, y = range, z/w = spot cone cosines.
            shader.setVec4("uLightParams[" + i + "]",
                    light.intensity(), light.range(), light.innerConeCos(), light.outerConeCos());
        }
    }

    private void setMaterial(GpuMaterial material) {
        MaterialData data = material.data();
        shader.setVec3("uBaseColor", data.baseColor());
        for (MaterialTexture slot : MaterialTexture.values()) {
            TextureBinding binding = material.texture(slot);
            shader.setInt(slot.texCoordUniform, binding.texCoord());
            binding.texture().bind(slot.unit());
            binding.sampler().bind(slot.unit());
        }

        MaterialData.AlphaMode alphaMode = data.alphaMode();
        shader.setInt("uAlphaMode", alphaMode.ordinal());
        shader.setFloat("uAlphaCutoff", data.alphaCutoff());
        if (alphaMode == MaterialData.AlphaMode.BLEND) {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDepthMask(false); // transparent surfaces don't hide what is behind them
        } else {
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDepthMask(true);
        }
    }

    @Override
    public void close() {
        shader.close();
    }
}
