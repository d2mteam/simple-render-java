package com.simplerender.asset;

/**
 * Declares the encoded color space for texture content.
 *
 * <p>Used to decide whether to sample in linear or sRGB space during upload and shading.
 */
public enum TextureColorSpace {
    LINEAR,
    SRGB
}
