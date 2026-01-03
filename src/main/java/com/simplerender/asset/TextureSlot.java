package com.simplerender.asset;

import java.util.Optional;

public final class TextureSlot {
    private final TextureData textureData;
    private final SamplerData samplerData;
    private final int texCoord;

    public TextureSlot(TextureData textureData, SamplerData samplerData) {
        this(textureData, samplerData, 0);
    }

    public TextureSlot(TextureData textureData, SamplerData samplerData, int texCoord) {
        if (textureData == null) {
            throw new IllegalArgumentException("Texture data is required");
        }
        this.textureData = textureData;
        this.samplerData = samplerData;
        this.texCoord = Math.max(texCoord, 0);
    }

    public TextureData textureData() {
        return textureData;
    }

    public Optional<SamplerData> samplerData() {
        return Optional.ofNullable(samplerData);
    }

    public int texCoord() {
        return texCoord;
    }
}
