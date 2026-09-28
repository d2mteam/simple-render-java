package com.simplerender.plugin.gltf;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.simplerender.asset.MaterialData;
import com.simplerender.asset.SamplerData;
import com.simplerender.asset.TextureColorSpace;
import com.simplerender.asset.TextureData;
import com.simplerender.asset.TextureSlot;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Turns glTF materials into {@link MaterialData}.
 *
 * <p>Materials and decoded images are cached, so primitives sharing a material (or
 * materials sharing an image) share the same objects, and the renderer uploads them once.
 */
final class GltfMaterialLoader {
    private static final Logger logger = LoggerFactory.getLogger(GltfMaterialLoader.class);
    private static final float[] DEFAULT_BASE_COLOR = { 0.8f, 0.8f, 0.8f };

    private record ImageKey(int imageIndex, TextureColorSpace colorSpace) {
    }

    private final GltfDocument document;
    private final Map<Integer, MaterialData> materials = new HashMap<>();
    private final Map<ImageKey, TextureData> images = new HashMap<>();

    GltfMaterialLoader(GltfDocument document) {
        this.document = document;
    }

    /** The material at {@code index}, or a plain grey material if the index is -1 or invalid. */
    MaterialData load(int index) {
        return materials.computeIfAbsent(index, this::read);
    }

    private MaterialData read(int index) {
        JsonObject material = document.object("materials", index);
        if (material == null) {
            return new MaterialData(DEFAULT_BASE_COLOR, null, null, null, null, null,
                    MaterialData.AlphaMode.OPAQUE, 0.5f);
        }
        JsonObject pbr = material.has("pbrMetallicRoughness")
                ? material.getAsJsonObject("pbrMetallicRoughness")
                : new JsonObject();

        float[] baseColor = DEFAULT_BASE_COLOR;
        if (pbr.has("baseColorFactor")) {
            JsonArray factor = pbr.getAsJsonArray("baseColorFactor"); // RGBA; alpha is not used yet
            baseColor = new float[] { factor.get(0).getAsFloat(), factor.get(1).getAsFloat(), factor.get(2).getAsFloat() };
        }
        TextureSlot baseColorTexture = texture(pbr, "baseColorTexture", TextureColorSpace.SRGB);
        TextureSlot metallicRoughness = texture(pbr, "metallicRoughnessTexture", TextureColorSpace.LINEAR);
        TextureSlot normal = texture(material, "normalTexture", TextureColorSpace.LINEAR);
        TextureSlot occlusion = texture(material, "occlusionTexture", TextureColorSpace.LINEAR);
        TextureSlot emissive = texture(material, "emissiveTexture", TextureColorSpace.SRGB);

        // KHR_materials_unlit: the colour should ignore lighting. The renderer has no unlit
        // mode, so show the base colour texture as emission (added after lighting) instead.
        boolean unlit = material.has("extensions")
                && material.getAsJsonObject("extensions").has("KHR_materials_unlit");
        if (unlit && baseColorTexture != null) {
            emissive = baseColorTexture;
            baseColorTexture = null;
            baseColor = new float[] { 0.0f, 0.0f, 0.0f };
        }

        MaterialData.AlphaMode alphaMode = MaterialData.AlphaMode.parse(
                material.has("alphaMode") ? material.get("alphaMode").getAsString() : null);
        float alphaCutoff = material.has("alphaCutoff") ? material.get("alphaCutoff").getAsFloat() : 0.5f;
        return new MaterialData(baseColor, baseColorTexture, normal, metallicRoughness, occlusion, emissive,
                alphaMode, alphaCutoff);
    }

    /** Reads a texture reference such as {@code owner.normalTexture}, or returns {@code null}. */
    private TextureSlot texture(JsonObject owner, String name, TextureColorSpace colorSpace) {
        if (!owner.has(name)) {
            return null;
        }
        JsonObject info = owner.getAsJsonObject(name);
        JsonObject texture = document.object("textures", info.get("index").getAsInt());
        if (texture == null || !texture.has("source")) {
            logger.warn("Texture {} has no image", info.get("index"));
            return null;
        }
        TextureData image = image(texture.get("source").getAsInt(), colorSpace);
        if (image == null) {
            return null;
        }
        SamplerData sampler = texture.has("sampler") ? sampler(texture.get("sampler").getAsInt()) : null;
        return new TextureSlot(image, sampler, GltfDocument.intOr(info, "texCoord", 0));
    }

    private SamplerData sampler(int index) {
        JsonObject sampler = document.object("samplers", index);
        if (sampler == null) {
            logger.warn("Sampler {} does not exist", index);
            return null;
        }
        return new SamplerData(
                GltfDocument.intOr(sampler, "minFilter", SamplerData.LINEAR),
                GltfDocument.intOr(sampler, "magFilter", SamplerData.LINEAR),
                GltfDocument.intOr(sampler, "wrapS", SamplerData.REPEAT),
                GltfDocument.intOr(sampler, "wrapT", SamplerData.REPEAT));
    }

    private TextureData image(int index, TextureColorSpace colorSpace) {
        ImageKey key = new ImageKey(index, colorSpace);
        if (!images.containsKey(key)) {
            images.put(key, decodeImage(index, colorSpace)); // caches failures (null) too
        }
        return images.get(key);
    }

    private TextureData decodeImage(int index, TextureColorSpace colorSpace) {
        JsonObject image = document.object("images", index);
        try {
            byte[] bytes;
            if (image != null && image.has("uri")) {
                bytes = document.readUri(image.get("uri").getAsString());
            } else if (image != null && image.has("bufferView")) {
                bytes = document.bufferViewBytes(image.get("bufferView").getAsInt());
            } else {
                logger.warn("Image {} has neither a uri nor a bufferView", index);
                return null;
            }
            BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(bytes));
            if (decoded == null) {
                logger.warn("Image {} is not in a format ImageIO can read", index);
                return null;
            }
            return toTextureData(decoded, colorSpace);
        } catch (Exception e) {
            logger.warn("Could not load image {}", index, e);
            return null;
        }
    }

    private static TextureData toTextureData(BufferedImage image, TextureColorSpace colorSpace) {
        int width = image.getWidth();
        int height = image.getHeight();
        byte[] rgba = new byte[width * height * 4];
        int i = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                rgba[i++] = (byte) (argb >> 16);
                rgba[i++] = (byte) (argb >> 8);
                rgba[i++] = (byte) argb;
                rgba[i++] = (byte) (argb >> 24);
            }
        }
        return new TextureData(width, height, rgba, colorSpace);
    }
}
