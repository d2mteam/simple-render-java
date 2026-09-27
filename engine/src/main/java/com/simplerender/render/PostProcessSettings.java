package com.simplerender.render;

/**
 * Switches and strengths for the screen-space effects in {@code screen_post.frag}.
 *
 * <p>A plain settings struct with public fields: create one, change what you need and pass
 * it to the engine. The engine keeps its own {@link #copy()}, so changing this object
 * afterwards has no effect until it is passed in again.
 */
public final class PostProcessSettings {
    public boolean toneMapping = true;
    public float exposure = 1.0f;

    public boolean bloom = true;
    public float bloomStrength = 0.35f;
    public float bloomThreshold = 1.0f;

    public boolean colorGrading = true;
    public float saturation = 1.0f;

    public boolean depthOfField = false;
    public float dofFocus = 0.4f;
    public float dofScale = 3.0f;

    public boolean motionBlur = false;
    public float motionBlurStrength = 0.35f;

    public boolean vignette = true;
    public float vignetteIntensity = 0.35f;

    public boolean filmGrain = true;
    public float filmGrainIntensity = 0.06f;

    public boolean ssao = false;
    public float ssaoStrength = 0.6f;
    public float ssaoRadius = 0.02f;

    public boolean contactShadows = false;
    public float contactShadowStrength = 0.5f;

    public PostProcessSettings copy() {
        PostProcessSettings c = new PostProcessSettings();
        c.toneMapping = toneMapping;
        c.exposure = exposure;
        c.bloom = bloom;
        c.bloomStrength = bloomStrength;
        c.bloomThreshold = bloomThreshold;
        c.colorGrading = colorGrading;
        c.saturation = saturation;
        c.depthOfField = depthOfField;
        c.dofFocus = dofFocus;
        c.dofScale = dofScale;
        c.motionBlur = motionBlur;
        c.motionBlurStrength = motionBlurStrength;
        c.vignette = vignette;
        c.vignetteIntensity = vignetteIntensity;
        c.filmGrain = filmGrain;
        c.filmGrainIntensity = filmGrainIntensity;
        c.ssao = ssao;
        c.ssaoStrength = ssaoStrength;
        c.ssaoRadius = ssaoRadius;
        c.contactShadows = contactShadows;
        c.contactShadowStrength = contactShadowStrength;
        return c;
    }
}
