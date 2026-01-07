package com.simplerender.gl;

/**
 * Collection of screen-space effects toggles and tuning parameters.
 *
 * <p>These settings feed into shader uniforms and post-processing passes to
 * enable effects such as tone mapping, bloom, and ray tracing blends.
 */
public final class ScreenSpaceSettings {
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
    private boolean rayTracingEnabled;
    private boolean rayTracingShadowsEnabled;
    private boolean rayTracingReflectionsEnabled;

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
    private float rayTracingMix = 0.0f;
    private int rayTracingMaxBounces = 2;

    private final float[] colorGradeTint = new float[]{1.0f, 1.0f, 1.0f};
    private final float[] motionBlurDirection = new float[]{1.0f, 0.0f};

    public boolean toneMappingEnabled() {
        return toneMappingEnabled;
    }

    public void setToneMappingEnabled(boolean toneMappingEnabled) {
        this.toneMappingEnabled = toneMappingEnabled;
    }

    public boolean bloomEnabled() {
        return bloomEnabled;
    }

    public void setBloomEnabled(boolean bloomEnabled) {
        this.bloomEnabled = bloomEnabled;
    }

    public boolean colorGradingEnabled() {
        return colorGradingEnabled;
    }

    public void setColorGradingEnabled(boolean colorGradingEnabled) {
        this.colorGradingEnabled = colorGradingEnabled;
    }

    public boolean depthOfFieldEnabled() {
        return depthOfFieldEnabled;
    }

    public void setDepthOfFieldEnabled(boolean depthOfFieldEnabled) {
        this.depthOfFieldEnabled = depthOfFieldEnabled;
    }

    public boolean motionBlurEnabled() {
        return motionBlurEnabled;
    }

    public void setMotionBlurEnabled(boolean motionBlurEnabled) {
        this.motionBlurEnabled = motionBlurEnabled;
    }

    public boolean vignetteEnabled() {
        return vignetteEnabled;
    }

    public void setVignetteEnabled(boolean vignetteEnabled) {
        this.vignetteEnabled = vignetteEnabled;
    }

    public boolean filmGrainEnabled() {
        return filmGrainEnabled;
    }

    public void setFilmGrainEnabled(boolean filmGrainEnabled) {
        this.filmGrainEnabled = filmGrainEnabled;
    }

    public boolean ssaoEnabled() {
        return ssaoEnabled;
    }

    public void setSsaoEnabled(boolean ssaoEnabled) {
        this.ssaoEnabled = ssaoEnabled;
    }

    public boolean ssrEnabled() {
        return ssrEnabled;
    }

    public void setSsrEnabled(boolean ssrEnabled) {
        this.ssrEnabled = ssrEnabled;
    }

    public boolean ssgiEnabled() {
        return ssgiEnabled;
    }

    public void setSsgiEnabled(boolean ssgiEnabled) {
        this.ssgiEnabled = ssgiEnabled;
    }

    public boolean contactShadowsEnabled() {
        return contactShadowsEnabled;
    }

    public void setContactShadowsEnabled(boolean contactShadowsEnabled) {
        this.contactShadowsEnabled = contactShadowsEnabled;
    }

    public boolean rayTracingEnabled() {
        return rayTracingEnabled;
    }

    public void setRayTracingEnabled(boolean rayTracingEnabled) {
        this.rayTracingEnabled = rayTracingEnabled;
    }

    public boolean rayTracingShadowsEnabled() {
        return rayTracingShadowsEnabled;
    }

    public void setRayTracingShadowsEnabled(boolean rayTracingShadowsEnabled) {
        this.rayTracingShadowsEnabled = rayTracingShadowsEnabled;
    }

    public boolean rayTracingReflectionsEnabled() {
        return rayTracingReflectionsEnabled;
    }

    public void setRayTracingReflectionsEnabled(boolean rayTracingReflectionsEnabled) {
        this.rayTracingReflectionsEnabled = rayTracingReflectionsEnabled;
    }

    public float exposure() {
        return exposure;
    }

    public void setExposure(float exposure) {
        this.exposure = exposure;
    }

    public float bloomStrength() {
        return bloomStrength;
    }

    public void setBloomStrength(float bloomStrength) {
        this.bloomStrength = bloomStrength;
    }

    public float bloomThreshold() {
        return bloomThreshold;
    }

    public void setBloomThreshold(float bloomThreshold) {
        this.bloomThreshold = bloomThreshold;
    }

    public float colorGradeSaturation() {
        return colorGradeSaturation;
    }

    public void setColorGradeSaturation(float colorGradeSaturation) {
        this.colorGradeSaturation = colorGradeSaturation;
    }

    public float vignetteIntensity() {
        return vignetteIntensity;
    }

    public void setVignetteIntensity(float vignetteIntensity) {
        this.vignetteIntensity = vignetteIntensity;
    }

    public float filmGrainIntensity() {
        return filmGrainIntensity;
    }

    public void setFilmGrainIntensity(float filmGrainIntensity) {
        this.filmGrainIntensity = filmGrainIntensity;
    }

    public float dofFocus() {
        return dofFocus;
    }

    public void setDofFocus(float dofFocus) {
        this.dofFocus = dofFocus;
    }

    public float dofScale() {
        return dofScale;
    }

    public void setDofScale(float dofScale) {
        this.dofScale = dofScale;
    }

    public float motionBlurStrength() {
        return motionBlurStrength;
    }

    public void setMotionBlurStrength(float motionBlurStrength) {
        this.motionBlurStrength = motionBlurStrength;
    }

    public float ssaoStrength() {
        return ssaoStrength;
    }

    public void setSsaoStrength(float ssaoStrength) {
        this.ssaoStrength = ssaoStrength;
    }

    public float ssaoRadius() {
        return ssaoRadius;
    }

    public void setSsaoRadius(float ssaoRadius) {
        this.ssaoRadius = ssaoRadius;
    }

    public float ssrStrength() {
        return ssrStrength;
    }

    public void setSsrStrength(float ssrStrength) {
        this.ssrStrength = ssrStrength;
    }

    public float ssgiStrength() {
        return ssgiStrength;
    }

    public void setSsgiStrength(float ssgiStrength) {
        this.ssgiStrength = ssgiStrength;
    }

    public float contactShadowStrength() {
        return contactShadowStrength;
    }

    public void setContactShadowStrength(float contactShadowStrength) {
        this.contactShadowStrength = contactShadowStrength;
    }

    public float rayTracingMix() {
        return rayTracingMix;
    }

    public void setRayTracingMix(float rayTracingMix) {
        this.rayTracingMix = rayTracingMix;
    }

    public int rayTracingMaxBounces() {
        return rayTracingMaxBounces;
    }

    public void setRayTracingMaxBounces(int rayTracingMaxBounces) {
        this.rayTracingMaxBounces = rayTracingMaxBounces;
    }

    public float[] colorGradeTint() {
        return colorGradeTint;
    }

    public float[] motionBlurDirection() {
        return motionBlurDirection;
    }
}
