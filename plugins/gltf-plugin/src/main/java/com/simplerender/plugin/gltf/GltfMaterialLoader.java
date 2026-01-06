package com.simplerender.plugin.gltf;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.simplerender.asset.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class GltfMaterialLoader {
    private static final Logger logger = LoggerFactory.getLogger(GltfMaterialLoader.class);

    private final JsonObject root;
    private final byte[][] bufferBytes;
    private final Path baseDir;
    private final Map<Integer, MaterialData> materialCache = new HashMap<>();
    private final Map<TextureCacheKey, TextureData> textureCache = new HashMap<>();

    public GltfMaterialLoader(JsonObject root, byte[][] bufferBytes, Path baseDir) {
        this.root = root;
        this.bufferBytes = bufferBytes;
        this.baseDir = baseDir;
    }

    public MaterialData loadMaterial(int materialIndex) {
        if (materialIndex >= 0 && materialCache.containsKey(materialIndex)) {
            return materialCache.get(materialIndex);
        }
        if (!root.has("materials") || materialIndex < 0 || materialIndex >= root.getAsJsonArray("materials").size()) {
            return defaultMaterial();
        }
        JsonObject material = root.getAsJsonArray("materials").get(materialIndex).getAsJsonObject();
        logger.info("Parsing Material {}: {}", materialIndex, material.toString());
        float[] baseColor = new float[] { 0.8f, 0.8f, 0.8f };
        TextureSlot baseColorTexture = null;
        TextureSlot normalTexture = null;
        TextureSlot metallicRoughnessTexture = null;
        TextureSlot aoTexture = null;
        TextureSlot emissiveTexture = null;
        if (material.has("pbrMetallicRoughness")) {
            JsonObject pbr = material.getAsJsonObject("pbrMetallicRoughness");
            if (pbr.has("baseColorFactor")) {
                JsonArray color = pbr.getAsJsonArray("baseColorFactor");
                baseColor = new float[] {
                        color.get(0).getAsFloat(),
                        color.get(1).getAsFloat(),
                        color.get(2).getAsFloat()
                };
            }
            if (pbr.has("baseColorTexture")) {
                JsonObject textureInfo = pbr.getAsJsonObject("baseColorTexture");
                int textureIndex = textureInfo.get("index").getAsInt();
                int texCoord = textureInfo.has("texCoord") ? textureInfo.get("texCoord").getAsInt() : 0;
                baseColorTexture = loadTextureByIndex(
                        textureIndex,
                        texCoord,
                        TextureColorSpace.SRGB);
            }
            if (pbr.has("metallicRoughnessTexture")) {
                JsonObject textureInfo = pbr.getAsJsonObject("metallicRoughnessTexture");
                int textureIndex = textureInfo.get("index").getAsInt();
                int texCoord = textureInfo.has("texCoord") ? textureInfo.get("texCoord").getAsInt() : 0;
                metallicRoughnessTexture = loadTextureByIndex(
                        textureIndex,
                        texCoord,
                        TextureColorSpace.LINEAR);
            }
        }
        if (material.has("normalTexture")) {
            JsonObject textureInfo = material.getAsJsonObject("normalTexture");
            int textureIndex = textureInfo.get("index").getAsInt();
            int texCoord = textureInfo.has("texCoord") ? textureInfo.get("texCoord").getAsInt() : 0;
            normalTexture = loadTextureByIndex(
                    textureIndex,
                    texCoord,
                    TextureColorSpace.LINEAR);
            if (normalTexture != null) {
                logger.info("Loaded Normal Map for Material {}: Index {}", materialIndex, textureIndex);
            } else {
                logger.warn("Failed to load Normal Map for Material {} (Index {})", materialIndex, textureIndex);
            }
        }
        if (material.has("occlusionTexture")) {
            JsonObject textureInfo = material.getAsJsonObject("occlusionTexture");
            int textureIndex = textureInfo.get("index").getAsInt();
            int texCoord = textureInfo.has("texCoord") ? textureInfo.get("texCoord").getAsInt() : 0;
            aoTexture = loadTextureByIndex(
                    textureIndex,
                    texCoord,
                    TextureColorSpace.LINEAR);
        }
        if (material.has("emissiveTexture")) {
            JsonObject textureInfo = material.getAsJsonObject("emissiveTexture");
            int textureIndex = textureInfo.get("index").getAsInt();
            int texCoord = textureInfo.has("texCoord") ? textureInfo.get("texCoord").getAsInt() : 0;
            emissiveTexture = loadTextureByIndex(
                    textureIndex,
                    texCoord,
                    TextureColorSpace.SRGB);
        }

        // Unlit Material Hack / Extension Support
        if (material.has("extensions") && material.getAsJsonObject("extensions").has("KHR_materials_unlit")) {
            logger.info("Material {} uses KHR_materials_unlit.", materialIndex);
            if (baseColorTexture != null) {
                // If there is a texture, we map it to Emissive to simulate Unlit behavior
                // (Emissive is added to result, bypassing lighting)
                emissiveTexture = baseColorTexture;
                // Clear base color texture and factor to avoid double-sampling or PBR lighting
                baseColorTexture = null;
                baseColor = new float[] { 0.0f, 0.0f, 0.0f };
                logger.info("Mapped BaseColor Texture to Emissive for Unlit Material {}.", materialIndex);
            } else {
                // If there is NO texture, we cannot easily map BaseColorFactor to
                // EmissiveFactor (unsupported).
                // If we set baseColor to black, it becomes invisible.
                // Fallback: Leave baseColor as is. It will be Lit (PBR), but at least visible.
                logger.warn("Unlit Material {} has no texture. Rendering as Lit PBR to preserve visibility.",
                        materialIndex);
            }
        }

        String alphaMode = material.has("alphaMode") ? material.get("alphaMode").getAsString() : "OPAQUE";
        float alphaCutoff = material.has("alphaCutoff") ? material.get("alphaCutoff").getAsFloat() : 0.5f;

        MaterialData materialData = new MaterialData(
                baseColor,
                baseColorTexture,
                normalTexture,
                metallicRoughnessTexture,
                aoTexture,
                emissiveTexture,
                alphaMode,
                alphaCutoff);
        materialCache.put(materialIndex, materialData);
        return materialData;
    }

    private MaterialData defaultMaterial() {
        return new MaterialData(
                new float[] { 0.8f, 0.8f, 0.8f },
                (TextureSlot) null,
                (TextureSlot) null,
                (TextureSlot) null,
                (TextureSlot) null,
                (TextureSlot) null,
                "OPAQUE",
                0.5f);
    }

    private TextureSlot loadTextureByIndex(
            int textureIndex,
            int texCoord,
            TextureColorSpace colorSpace) {
        if (!root.has("textures") || !root.has("images")) {
            logger.warn("glTF texture referenced but textures/images arrays are missing");
            return null;
        }
        JsonObject texture = root.getAsJsonArray("textures").get(textureIndex).getAsJsonObject();
        if (!texture.has("source")) {
            logger.warn("glTF texture {} has no source index", textureIndex);
            return null;
        }
        int sourceIndex = texture.get("source").getAsInt();
        SamplerData samplerData = null;
        if (texture.has("sampler")) {
            int samplerIndex = texture.get("sampler").getAsInt();
            samplerData = loadSamplerByIndex(samplerIndex);
        }
        TextureCacheKey cacheKey = new TextureCacheKey(sourceIndex, colorSpace);
        if (textureCache.containsKey(cacheKey)) {
            return new TextureSlot(textureCache.get(cacheKey), samplerData, texCoord);
        }
        JsonObject image = root.getAsJsonArray("images").get(sourceIndex).getAsJsonObject();
        try {
            byte[] imageBytes = readImageBytes(image);
            if (imageBytes == null) {
                return null;
            }
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (bufferedImage == null) {
                logger.warn("Failed to decode glTF texture image {}", sourceIndex);
                return null;
            }
            logger.info("Loaded glTF texture image {} ({}x{})", sourceIndex, bufferedImage.getWidth(),
                    bufferedImage.getHeight());
            TextureData textureData = buildTextureData(bufferedImage, colorSpace);
            textureCache.put(cacheKey, textureData);
            return new TextureSlot(textureData, samplerData, texCoord);
        } catch (Exception e) {
            logger.warn("Failed to load glTF texture image {}", sourceIndex, e);
            return null;
        }
    }

    private SamplerData loadSamplerByIndex(int samplerIndex) {
        if (!root.has("samplers")) {
            logger.warn("glTF sampler referenced but samplers array is missing");
            return null;
        }
        JsonArray samplers = root.getAsJsonArray("samplers");
        if (samplerIndex < 0 || samplerIndex >= samplers.size()) {
            logger.warn("glTF sampler index {} out of range", samplerIndex);
            return null;
        }
        JsonObject sampler = samplers.get(samplerIndex).getAsJsonObject();
        int minFilter = sampler.has("minFilter") ? sampler.get("minFilter").getAsInt() : SamplerData.LINEAR;
        int magFilter = sampler.has("magFilter") ? sampler.get("magFilter").getAsInt() : SamplerData.LINEAR;
        int wrapS = sampler.has("wrapS") ? sampler.get("wrapS").getAsInt() : SamplerData.REPEAT;
        int wrapT = sampler.has("wrapT") ? sampler.get("wrapT").getAsInt() : SamplerData.REPEAT;
        return new SamplerData(minFilter, magFilter, wrapS, wrapT);
    }

    private byte[] readImageBytes(JsonObject image) throws Exception {
        if (image.has("uri")) {
            String uri = image.get("uri").getAsString();
            return decodeUri(uri, baseDir);
        }
        if (!image.has("bufferView")) {
            logger.warn("glTF image missing uri and bufferView");
            return null;
        }
        int bufferViewIndex = image.get("bufferView").getAsInt();
        JsonObject bufferView = root.getAsJsonArray("bufferViews").get(bufferViewIndex).getAsJsonObject();
        byte[] viewBufferBytes = bufferBytesForView(bufferView, bufferBytes);
        int offset = bufferView.has("byteOffset") ? bufferView.get("byteOffset").getAsInt() : 0;
        int length = bufferView.get("byteLength").getAsInt();
        if (offset + length > viewBufferBytes.length) {
            throw new IllegalArgumentException("glTF image bufferView exceeds buffer length");
        }
        byte[] imageBytes = new byte[length];
        System.arraycopy(viewBufferBytes, offset, imageBytes, 0, length);
        return imageBytes;
    }

    private byte[] decodeUri(String uri, Path base) throws Exception {
        if (uri.startsWith("data:")) {
            String base64 = uri.substring(uri.indexOf(',') + 1);
            return Base64.getDecoder().decode(base64);
        }
        return Files.readAllBytes(base.resolve(uri));
    }

    private byte[] bufferBytesForView(JsonObject bufferView, byte[][] bufferBytes) {
        int bufferIndex = bufferView.has("buffer") ? bufferView.get("buffer").getAsInt() : 0;
        if (bufferIndex < 0 || bufferIndex >= bufferBytes.length || bufferBytes[bufferIndex] == null) {
            throw new IllegalArgumentException("glTF buffer index out of range: " + bufferIndex);
        }
        return bufferBytes[bufferIndex];
    }

    private TextureData buildTextureData(BufferedImage image, TextureColorSpace colorSpace) {
        int width = image.getWidth();
        int height = image.getHeight();
        byte[] rgba = new byte[width * height * 4];
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                rgba[index++] = (byte) ((argb >> 16) & 0xFF);
                rgba[index++] = (byte) ((argb >> 8) & 0xFF);
                rgba[index++] = (byte) (argb & 0xFF);
                rgba[index++] = (byte) ((argb >> 24) & 0xFF);
            }
        }
        return new TextureData(width, height, rgba, colorSpace);
    }

    private static final class TextureCacheKey {
        private final int imageIndex;
        private final TextureColorSpace colorSpace;

        private TextureCacheKey(int imageIndex, TextureColorSpace colorSpace) {
            this.imageIndex = imageIndex;
            this.colorSpace = colorSpace;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            TextureCacheKey other = (TextureCacheKey) obj;
            return imageIndex == other.imageIndex && colorSpace == other.colorSpace;
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(imageIndex);
            result = 31 * result + colorSpace.hashCode();
            return result;
        }
    }
}
