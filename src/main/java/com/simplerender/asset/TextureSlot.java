package com.simplerender.asset;

import java.util.Optional;

public final class TextureSlot {
    private final TextureData textureData;
    private final SamplerData samplerData;

    public TextureSlot(TextureData textureData, SamplerData samplerData) {
        if (textureData == null) {
            throw new IllegalArgumentException("Texture data is required");
        }
        this.textureData = textureData;
        this.samplerData = samplerData;
    }

    public TextureData textureData() {
        return textureData;
    }

    public Optional<SamplerData> samplerData() {
        return Optional.ofNullable(samplerData);
    }
}
