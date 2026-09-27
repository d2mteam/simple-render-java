package com.simplerender.asset;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * CPU-side description of a PBR material: a base colour plus optional texture slots.
 *
 * <p>Immutable. The renderer uploads it to the GPU the first time it is drawn.
 */
public final class MaterialData {
    /** How the material's alpha is used (same meaning as glTF {@code alphaMode}). */
    public enum AlphaMode {
        OPAQUE,
        MASK,
        BLEND;

        /** Parses a glTF alpha mode name; anything unknown is treated as {@link #OPAQUE}. */
        public static AlphaMode parse(String name) {
            if (name == null) {
                return OPAQUE;
            }
            try {
                return valueOf(name.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return OPAQUE;
            }
        }
    }

    private final float[] baseColor;
    private final TextureSlot baseColorTexture;
    private final TextureSlot normalTexture;
    private final TextureSlot metallicRoughnessTexture;
    private final TextureSlot aoTexture;
    private final TextureSlot emissiveTexture;
    private final AlphaMode alphaMode;
    private final float alphaCutoff;

    /** Simple opaque material with an optional base colour texture (sRGB). */
    public MaterialData(float[] baseColor, TextureData baseColorTexture) {
        this(
                baseColor,
                baseColorTexture == null
                        ? null
                        : new TextureSlot(baseColorTexture.withColorSpace(TextureColorSpace.SRGB), null),
                null,
                null,
                null,
                null,
                AlphaMode.OPAQUE,
                0.5f);
    }

    /** Full material. Any texture slot may be {@code null}. */
    public MaterialData(
            float[] baseColor,
            TextureSlot baseColorTexture,
            TextureSlot normalTexture,
            TextureSlot metallicRoughnessTexture,
            TextureSlot aoTexture,
            TextureSlot emissiveTexture,
            AlphaMode alphaMode,
            float alphaCutoff) {
        if (baseColor.length != 3) {
            throw new IllegalArgumentException("Base color must have 3 components");
        }
        this.baseColor = Arrays.copyOf(baseColor, baseColor.length);
        this.baseColorTexture = baseColorTexture;
        this.normalTexture = normalTexture;
        this.metallicRoughnessTexture = metallicRoughnessTexture;
        this.aoTexture = aoTexture;
        this.emissiveTexture = emissiveTexture;
        this.alphaMode = alphaMode == null ? AlphaMode.OPAQUE : alphaMode;
        this.alphaCutoff = alphaCutoff;
    }

    public float[] baseColor() {
        return Arrays.copyOf(baseColor, baseColor.length);
    }

    public Optional<TextureSlot> baseColorTexture() {
        return Optional.ofNullable(baseColorTexture);
    }

    public Optional<TextureSlot> normalTexture() {
        return Optional.ofNullable(normalTexture);
    }

    public Optional<TextureSlot> metallicRoughnessTexture() {
        return Optional.ofNullable(metallicRoughnessTexture);
    }

    public Optional<TextureSlot> aoTexture() {
        return Optional.ofNullable(aoTexture);
    }

    public Optional<TextureSlot> emissiveTexture() {
        return Optional.ofNullable(emissiveTexture);
    }

    public AlphaMode alphaMode() {
        return alphaMode;
    }

    public float alphaCutoff() {
        return alphaCutoff;
    }
}
