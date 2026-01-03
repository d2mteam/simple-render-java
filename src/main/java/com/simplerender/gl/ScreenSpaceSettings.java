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

    void setToneMappingEnabled(boolean toneMappingEnabled) {
        this.toneMappingEnabled = toneMappingEnabled;
    }

    public boolean bloomEnabled() {
        return bloomEnabled;
    }

    void setBloomEnabled(boolean bloomEnabled) {
        this.bloomEnabled = bloomEnabled;
    }

    public boolean colorGradingEnabled() {
        return colorGradingEnabled;
    }

    void setColorGradingEnabled(boolean colorGradingEnabled) {
        this.colorGradingEnabled = colorGradingEnabled;
    }

    public boolean depthOfFieldEnabled() {
        return depthOfFieldEnabled;
    }

    void setDepthOfFieldEnabled(boolean depthOfFieldEnabled) {
        this.depthOfFieldEnabled = depthOfFieldEnabled;
    }

    public boolean motionBlurEnabled() {
        return motionBlurEnabled;
    }

    void setMotionBlurEnabled(boolean motionBlurEnabled) {
        this.motionBlurEnabled = motionBlurEnabled;
    }

    public boolean vignetteEnabled() {
        return vignetteEnabled;
    }

    void setVignetteEnabled(boolean vignetteEnabled) {
        this.vignetteEnabled = vignetteEnabled;
    }

    public boolean filmGrainEnabled() {
        return filmGrainEnabled;
    }

    void setFilmGrainEnabled(boolean filmGrainEnabled) {
        this.filmGrainEnabled = filmGrainEnabled;
    }

    public boolean ssaoEnabled() {
        return ssaoEnabled;
    }

    void setSsaoEnabled(boolean ssaoEnabled) {
        this.ssaoEnabled = ssaoEnabled;
    }

    public boolean ssrEnabled() {
        return ssrEnabled;
    }

    void setSsrEnabled(boolean ssrEnabled) {
        this.ssrEnabled = ssrEnabled;
    }

    public boolean ssgiEnabled() {
        return ssgiEnabled;
    }

    void setSsgiEnabled(boolean ssgiEnabled) {
        this.ssgiEnabled = ssgiEnabled;
    }

    public boolean contactShadowsEnabled() {
        return contactShadowsEnabled;
    }

    void setContactShadowsEnabled(boolean contactShadowsEnabled) {
        this.contactShadowsEnabled = contactShadowsEnabled;
    }

    public boolean rayTracingEnabled() {
        return rayTracingEnabled;
    }

    void setRayTracingEnabled(boolean rayTracingEnabled) {
        this.rayTracingEnabled = rayTracingEnabled;
    }

    public boolean rayTracingShadowsEnabled() {
        return rayTracingShadowsEnabled;
    }

    void setRayTracingShadowsEnabled(boolean rayTracingShadowsEnabled) {
        this.rayTracingShadowsEnabled = rayTracingShadowsEnabled;
    }

    public boolean rayTracingReflectionsEnabled() {
        return rayTracingReflectionsEnabled;
    }

    void setRayTracingReflectionsEnabled(boolean rayTracingReflectionsEnabled) {
        this.rayTracingReflectionsEnabled = rayTracingReflectionsEnabled;
    }

    public float exposure() {
        return exposure;
    }

    void setExposure(float exposure) {
        this.exposure = exposure;
    }

    public float bloomStrength() {
        return bloomStrength;
    }

    void setBloomStrength(float bloomStrength) {
        this.bloomStrength = bloomStrength;
    }

    public float bloomThreshold() {
        return bloomThreshold;
    }

    void setBloomThreshold(float bloomThreshold) {
        this.bloomThreshold = bloomThreshold;
    }

    public float colorGradeSaturation() {
        return colorGradeSaturation;
    }

    void setColorGradeSaturation(float colorGradeSaturation) {
        this.colorGradeSaturation = colorGradeSaturation;
    }

    public float vignetteIntensity() {
        return vignetteIntensity;
    }

    void setVignetteIntensity(float vignetteIntensity) {
        this.vignetteIntensity = vignetteIntensity;
    }

    public float filmGrainIntensity() {
        return filmGrainIntensity;
    }

    void setFilmGrainIntensity(float filmGrainIntensity) {
        this.filmGrainIntensity = filmGrainIntensity;
    }

    public float dofFocus() {
        return dofFocus;
    }

    void setDofFocus(float dofFocus) {
        this.dofFocus = dofFocus;
    }

    public float dofScale() {
        return dofScale;
    }

    void setDofScale(float dofScale) {
        this.dofScale = dofScale;
    }

    public float motionBlurStrength() {
        return motionBlurStrength;
    }

    void setMotionBlurStrength(float motionBlurStrength) {
        this.motionBlurStrength = motionBlurStrength;
    }

    public float ssaoStrength() {
        return ssaoStrength;
    }

    void setSsaoStrength(float ssaoStrength) {
        this.ssaoStrength = ssaoStrength;
    }

    public float ssaoRadius() {
        return ssaoRadius;
    }

    void setSsaoRadius(float ssaoRadius) {
        this.ssaoRadius = ssaoRadius;
    }

    public float ssrStrength() {
        return ssrStrength;
    }

    void setSsrStrength(float ssrStrength) {
        this.ssrStrength = ssrStrength;
    }

    public float ssgiStrength() {
        return ssgiStrength;
    }

    void setSsgiStrength(float ssgiStrength) {
        this.ssgiStrength = ssgiStrength;
    }

    public float contactShadowStrength() {
        return contactShadowStrength;
    }

    void setContactShadowStrength(float contactShadowStrength) {
        this.contactShadowStrength = contactShadowStrength;
    }

    public float rayTracingMix() {
        return rayTracingMix;
    }

    void setRayTracingMix(float rayTracingMix) {
        this.rayTracingMix = rayTracingMix;
    }

    public int rayTracingMaxBounces() {
        return rayTracingMaxBounces;
    }

    void setRayTracingMaxBounces(int rayTracingMaxBounces) {
        this.rayTracingMaxBounces = rayTracingMaxBounces;
    }

    public float[] colorGradeTint() {
        return colorGradeTint;
    }

    public float[] motionBlurDirection() {
        return motionBlurDirection;
    }
}
