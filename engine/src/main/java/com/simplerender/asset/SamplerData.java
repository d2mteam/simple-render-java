package com.simplerender.asset;

/**
 * How a texture is sampled: filtering and wrapping, using the glTF / OpenGL enum values.
 *
 * <p>This is a record, so two samplers with the same settings are equal and the renderer
 * can share a single GPU sampler object between them.
 */
public record SamplerData(int minFilter, int magFilter, int wrapS, int wrapT) {
    public static final int NEAREST = 9728;
    public static final int LINEAR = 9729;
    public static final int NEAREST_MIPMAP_NEAREST = 9984;
    public static final int LINEAR_MIPMAP_NEAREST = 9985;
    public static final int NEAREST_MIPMAP_LINEAR = 9986;
    public static final int LINEAR_MIPMAP_LINEAR = 9987;
    public static final int REPEAT = 10497;
    public static final int CLAMP_TO_EDGE = 33071;
    public static final int MIRRORED_REPEAT = 33648;

    public static SamplerData defaults() {
        return new SamplerData(LINEAR, LINEAR, REPEAT, REPEAT);
    }
}
