package com.simplerender.gl;

final class ScreenSpaceSettings {
    private boolean toneMappingEnabled = true;
    private boolean bloomEnabled = true;
    private boolean colorGradingEnabled = true;
    private boolean depthOfFieldEnabled;
    private boolean motionBlurEnabled;
    private boolean vignetteEnabled = true;
    private boolean filmGrainEnabled = true;
    private boolean ssaoEnabled;
    private boolean ssrEnabled;
    private boolean ssgiEnabled;
    private boolean contactShadowsEnabled;
    private boolean rayTracingEnabled = true;
    private boolean rayTracingShadowsEnabled = true;
    private boolean rayTracingReflectionsEnabled = true;

    private float exposure = 1.0f;
    private float bloomStrength = 0.35f;
    private float bloomThreshold = 1.0f;
    private float colorGradeSaturation = 1.0f;
    private float vignetteIntensity = 0.35f;
    private float filmGrainIntensity = 0.06f;
    private float dofFocus = 0.4f;
    private float dofScale = 3.0f;
    private float motionBlurStrength = 0.35f;
    private float ssaoStrength = 0.6f;
    private float ssaoRadius = 0.02f;
    private float ssrStrength = 0.35f;
    private float ssgiStrength = 0.35f;
    private float contactShadowStrength = 0.5f;
    private float rayTracingMix = 0.85f;
    private int rayTracingMaxBounces = 2;

    private final float[] colorGradeTint = new float[] { 1.0f, 1.0f, 1.0f };
    private final float[] motionBlurDirection = new float[] { 1.0f, 0.0f };

    public boolean toneMappingEnabled() {
        return toneMappingEnabled;
    }

    public boolean bloomEnabled() {
        return bloomEnabled;
    }

    public boolean colorGradingEnabled() {
        return colorGradingEnabled;
    }

    public boolean depthOfFieldEnabled() {
        return depthOfFieldEnabled;
    }

    public boolean motionBlurEnabled() {
        return motionBlurEnabled;
    }

    public boolean vignetteEnabled() {
        return vignetteEnabled;
    }

    public boolean filmGrainEnabled() {
        return filmGrainEnabled;
    }

    public boolean ssaoEnabled() {
        return ssaoEnabled;
    }

    public boolean ssrEnabled() {
        return ssrEnabled;
    }

    public boolean ssgiEnabled() {
        return ssgiEnabled;
    }

    public boolean contactShadowsEnabled() {
        return contactShadowsEnabled;
    }

    public boolean rayTracingEnabled() {
        return rayTracingEnabled;
    }

    public boolean rayTracingShadowsEnabled() {
        return rayTracingShadowsEnabled;
    }

    public boolean rayTracingReflectionsEnabled() {
        return rayTracingReflectionsEnabled;
    }

    public float exposure() {
        return exposure;
    }

    public float bloomStrength() {
        return bloomStrength;
    }

    public float bloomThreshold() {
        return bloomThreshold;
    }

    public float colorGradeSaturation() {
        return colorGradeSaturation;
    }

    public float vignetteIntensity() {
        return vignetteIntensity;
    }

    public float filmGrainIntensity() {
        return filmGrainIntensity;
    }

    public float dofFocus() {
        return dofFocus;
    }

    public float dofScale() {
        return dofScale;
    }

    public float motionBlurStrength() {
        return motionBlurStrength;
    }

    public float ssaoStrength() {
        return ssaoStrength;
    }

    public float ssaoRadius() {
        return ssaoRadius;
    }

    public float ssrStrength() {
        return ssrStrength;
    }

    public float ssgiStrength() {
        return ssgiStrength;
    }

    public float contactShadowStrength() {
        return contactShadowStrength;
    }

    public float rayTracingMix() {
        return rayTracingMix;
    }

    public int rayTracingMaxBounces() {
        return rayTracingMaxBounces;
    }

    public float[] colorGradeTint() {
        return colorGradeTint;
    }

    public float[] motionBlurDirection() {
        return motionBlurDirection;
    }
}
