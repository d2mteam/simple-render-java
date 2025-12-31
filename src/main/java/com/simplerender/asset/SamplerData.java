package com.simplerender.asset;

public final class SamplerData {
    public static final int NEAREST = 9728;
    public static final int LINEAR = 9729;
    public static final int NEAREST_MIPMAP_NEAREST = 9984;
    public static final int LINEAR_MIPMAP_NEAREST = 9985;
    public static final int NEAREST_MIPMAP_LINEAR = 9986;
    public static final int LINEAR_MIPMAP_LINEAR = 9987;
    public static final int REPEAT = 10497;
    public static final int CLAMP_TO_EDGE = 33071;
    public static final int MIRRORED_REPEAT = 33648;

    private final int minFilter;
    private final int magFilter;
    private final int wrapS;
    private final int wrapT;

    public SamplerData(int minFilter, int magFilter, int wrapS, int wrapT) {
        this.minFilter = minFilter;
        this.magFilter = magFilter;
        this.wrapS = wrapS;
        this.wrapT = wrapT;
    }

    public static SamplerData defaults() {
        return new SamplerData(LINEAR, LINEAR, REPEAT, REPEAT);
    }

    public int minFilter() {
        return minFilter;
    }

    public int magFilter() {
        return magFilter;
    }

    public int wrapS() {
        return wrapS;
    }

    public int wrapT() {
        return wrapT;
    }
}
