package com.simplerender.render;

import com.simplerender.render.gl.FullscreenQuad;
import com.simplerender.render.gl.RenderTarget;
import com.simplerender.render.gl.ShaderProgram;
import org.lwjgl.opengl.GL11;

/**
 * Pass 2: reads the lit scene (colour + depth) and applies screen-space effects
 * (bloom, tone mapping, vignette, ...) by drawing one full-screen quad.
 */
final class PostProcessPass implements AutoCloseable {
    private static final int SCENE_COLOR_UNIT = 0;
    private static final int SCENE_DEPTH_UNIT = 1;

    private final ShaderProgram shader = ShaderLibrary.loadPostProcessShader();
    private final FullscreenQuad quad = new FullscreenQuad();
    private int frameIndex; // seeds the film grain noise

    PostProcessPass() {
        shader.use();
        shader.setInt("uSceneColor", SCENE_COLOR_UNIT);
        shader.setInt("uSceneDepth", SCENE_DEPTH_UNIT);
    }

    void render(RenderTarget scene, RenderTarget target, PostProcessSettings settings) {
        target.bind();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        shader.use();
        shader.setInt("uFrameIndex", frameIndex++);
        shader.setVec2("uTexelSize", 1.0f / scene.width(), 1.0f / scene.height());
        setEffectUniforms(settings);
        scene.bindColorTexture(SCENE_COLOR_UNIT);
        scene.bindDepthTexture(SCENE_DEPTH_UNIT);
        quad.draw();
    }

    private void setEffectUniforms(PostProcessSettings s) {
        shader.setInt("uEnableToneMap", s.toneMapping ? 1 : 0);
        shader.setFloat("uExposure", s.exposure);

        shader.setInt("uEnableBloom", s.bloom ? 1 : 0);
        shader.setFloat("uBloomStrength", s.bloomStrength);
        shader.setFloat("uBloomThreshold", s.bloomThreshold);

        shader.setInt("uEnableColorGrade", s.colorGrading ? 1 : 0);
        shader.setFloat("uColorGradeSaturation", s.saturation);

        shader.setInt("uEnableDof", s.depthOfField ? 1 : 0);
        shader.setFloat("uDofFocus", s.dofFocus);
        shader.setFloat("uDofScale", s.dofScale);

        shader.setInt("uEnableMotionBlur", s.motionBlur ? 1 : 0);
        shader.setFloat("uMotionBlurStrength", s.motionBlurStrength);
        shader.setVec2("uMotionBlurDir", 1.0f, 0.0f);

        shader.setInt("uEnableVignette", s.vignette ? 1 : 0);
        shader.setFloat("uVignetteIntensity", s.vignetteIntensity);

        shader.setInt("uEnableFilmGrain", s.filmGrain ? 1 : 0);
        shader.setFloat("uFilmGrainIntensity", s.filmGrainIntensity);

        shader.setInt("uEnableSsao", s.ssao ? 1 : 0);
        shader.setFloat("uSsaoStrength", s.ssaoStrength);
        shader.setFloat("uSsaoRadius", s.ssaoRadius);

        shader.setInt("uEnableContactShadows", s.contactShadows ? 1 : 0);
        shader.setFloat("uContactShadowStrength", s.contactShadowStrength);
    }

    @Override
    public void close() {
        shader.close();
        quad.close();
    }
}
