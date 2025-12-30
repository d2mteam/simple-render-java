package com.simplerender.plugin.gltf;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplerender.asset.MaterialData;
import com.simplerender.asset.MeshData;
import com.simplerender.asset.TextureData;
import com.simplerender.asset.plugin.ModelImporter;
import org.pf4j.Extension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;

@Extension
public final class GltfModelImporter implements ModelImporter {
    @Override
    public String[] supportedExtensions() {
        return new String[] {"gltf", "glb"};
    }

    @Override
    public ImportedModel importModel(Path path) {
        try {
            GltfAsset asset = loadAsset(path);
            JsonObject root = asset.root();
            byte[] bufferBytes = asset.buffer();

            JsonArray bufferViews = root.getAsJsonArray("bufferViews");
            JsonArray accessors = root.getAsJsonArray("accessors");
            JsonObject mesh = root.getAsJsonArray("meshes").get(0).getAsJsonObject();
            JsonObject primitive = mesh.getAsJsonArray("primitives").get(0).getAsJsonObject();
            JsonObject attributes = primitive.getAsJsonObject("attributes");

            int positionAccessorIndex = attributes.get("POSITION").getAsInt();
            int normalAccessorIndex = attributes.has("NORMAL") ? attributes.get("NORMAL").getAsInt() : -1;
            int indexAccessorIndex = primitive.has("indices") ? primitive.get("indices").getAsInt() : -1;

            float[] positions = readFloatVec3(accessors, bufferViews, bufferBytes, positionAccessorIndex);
            float[] normals = normalAccessorIndex >= 0
                ? readFloatVec3(accessors, bufferViews, bufferBytes, normalAccessorIndex)
                : defaultNormals(positions.length / 3);
            int[] indices = indexAccessorIndex >= 0
                ? readIndices(accessors, bufferViews, bufferBytes, indexAccessorIndex)
                : sequentialIndices(positions.length / 3);

            float[] baseColor = new float[] {0.8f, 0.8f, 0.8f};
            TextureData baseColorTexture = null;
            TextureData metallicRoughnessTexture = null;
            TextureData normalTexture = null;
            TextureData occlusionTexture = null;
            TextureData emissiveTexture = null;
            if (root.has("materials")) {
                JsonObject material = root.getAsJsonArray("materials").get(0).getAsJsonObject();
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
                        baseColorTexture = readTexture(root, bufferViews, bufferBytes, path.getParent(),
                            pbr.getAsJsonObject("baseColorTexture"));
                    }
                    if (pbr.has("metallicRoughnessTexture")) {
                        metallicRoughnessTexture = readTexture(root, bufferViews, bufferBytes, path.getParent(),
                            pbr.getAsJsonObject("metallicRoughnessTexture"));
                    }
                }
                if (material.has("normalTexture")) {
                    normalTexture = readTexture(root, bufferViews, bufferBytes, path.getParent(),
                        material.getAsJsonObject("normalTexture"));
                }
                if (material.has("occlusionTexture")) {
                    occlusionTexture = readTexture(root, bufferViews, bufferBytes, path.getParent(),
                        material.getAsJsonObject("occlusionTexture"));
                }
                if (material.has("emissiveTexture")) {
                    emissiveTexture = readTexture(root, bufferViews, bufferBytes, path.getParent(),
                        material.getAsJsonObject("emissiveTexture"));
                }
            }

            MeshData meshData = new MeshData(positions, normals, indices);
            MaterialData materialData = new MaterialData(
                baseColor,
                baseColorTexture,
                metallicRoughnessTexture,
                normalTexture,
                occlusionTexture,
                emissiveTexture
            );
            return new ImportedModel(meshData, materialData);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to import glTF: " + path, e);
        }
    }

    private GltfAsset loadAsset(Path path) throws Exception {
        if (path.toString().toLowerCase().endsWith(".glb")) {
            return loadGlb(path);
        }
        String json = Files.readString(path);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray buffers = root.getAsJsonArray("buffers");
        JsonObject buffer = buffers.get(0).getAsJsonObject();
        String uri = buffer.get("uri").getAsString();
        byte[] bufferBytes = decodeUri(uri, path.getParent());
        return new GltfAsset(root, bufferBytes);
    }

    private GltfAsset loadGlb(Path path) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int magic = buffer.getInt();
        if (magic != 0x46546C67) { // "glTF"
            throw new IllegalArgumentException("Invalid GLB header");
        }
        buffer.getInt(); // version
        buffer.getInt(); // length
        int jsonLength = buffer.getInt();
        int jsonType = buffer.getInt();
        if (jsonType != 0x4E4F534A) { // "JSON"
            throw new IllegalArgumentException("Missing JSON chunk");
        }
        byte[] jsonBytes = new byte[jsonLength];
        buffer.get(jsonBytes);
        String json = new String(jsonBytes);

        int binLength = buffer.getInt();
        int binType = buffer.getInt();
        if (binType != 0x004E4942) { // "BIN"
            throw new IllegalArgumentException("Missing BIN chunk");
        }
        byte[] binBytes = new byte[binLength];
        buffer.get(binBytes);

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        return new GltfAsset(root, binBytes);
    }

    private byte[] decodeUri(String uri, Path base) throws Exception {
        if (uri.startsWith("data:")) {
            String base64 = uri.substring(uri.indexOf(',') + 1);
            return Base64.getDecoder().decode(base64);
        }
        return Files.readAllBytes(base.resolve(uri));
    }

    private TextureData readTexture(
        JsonObject root,
        JsonArray bufferViews,
        byte[] bufferBytes,
        Path basePath,
        JsonObject textureInfo
    ) throws Exception {
        if (textureInfo == null || !textureInfo.has("index")) {
            return null;
        }
        if (!root.has("textures") || !root.has("images")) {
            return null;
        }
        int textureIndex = textureInfo.get("index").getAsInt();
        JsonArray textures = root.getAsJsonArray("textures");
        if (textureIndex < 0 || textureIndex >= textures.size()) {
            return null;
        }
        JsonObject texture = textures.get(textureIndex).getAsJsonObject();
        if (!texture.has("source")) {
            return null;
        }
        int imageIndex = texture.get("source").getAsInt();
        JsonArray images = root.getAsJsonArray("images");
        if (imageIndex < 0 || imageIndex >= images.size()) {
            return null;
        }
        JsonObject image = images.get(imageIndex).getAsJsonObject();
        byte[] imageBytes = readImageBytes(bufferViews, bufferBytes, basePath, image);
        if (imageBytes == null) {
            return null;
        }
        return decodeImageToTexture(imageBytes);
    }

    private byte[] readImageBytes(
        JsonArray bufferViews,
        byte[] bufferBytes,
        Path basePath,
        JsonObject image
    ) throws Exception {
        if (image.has("uri")) {
            return decodeUri(image.get("uri").getAsString(), basePath);
        }
        if (image.has("bufferView")) {
            int bufferViewIndex = image.get("bufferView").getAsInt();
            if (bufferViewIndex < 0 || bufferViewIndex >= bufferViews.size()) {
                return null;
            }
            JsonObject bufferView = bufferViews.get(bufferViewIndex).getAsJsonObject();
            int viewOffset = bufferView.has("byteOffset") ? bufferView.get("byteOffset").getAsInt() : 0;
            int length = bufferView.get("byteLength").getAsInt();
            if (bufferBytes == null || bufferBytes.length < viewOffset + length) {
                return null;
            }
            return Arrays.copyOfRange(bufferBytes, viewOffset, viewOffset + length);
        }
        return null;
    }

    private TextureData decodeImageToTexture(byte[] imageBytes) throws Exception {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (image == null) {
            return null;
        }
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
        return new TextureData(width, height, rgba);
    }

    private float[] readFloatVec3(JsonArray accessors, JsonArray bufferViews, byte[] bufferBytes, int accessorIndex) {
        JsonObject accessor = accessors.get(accessorIndex).getAsJsonObject();
        int count = accessor.get("count").getAsInt();
        int bufferViewIndex = accessor.get("bufferView").getAsInt();
        int byteOffset = accessor.has("byteOffset") ? accessor.get("byteOffset").getAsInt() : 0;

        JsonObject bufferView = bufferViews.get(bufferViewIndex).getAsJsonObject();
        int viewOffset = bufferView.has("byteOffset") ? bufferView.get("byteOffset").getAsInt() : 0;
        int stride = bufferView.has("byteStride") ? bufferView.get("byteStride").getAsInt() : 12;

        ByteBuffer buffer = ByteBuffer.wrap(bufferBytes).order(ByteOrder.LITTLE_ENDIAN);
        float[] values = new float[count * 3];
        for (int i = 0; i < count; i++) {
            int base = viewOffset + byteOffset + i * stride;
            values[i * 3] = buffer.getFloat(base);
            values[i * 3 + 1] = buffer.getFloat(base + 4);
            values[i * 3 + 2] = buffer.getFloat(base + 8);
        }
        return values;
    }

    private int[] readIndices(JsonArray accessors, JsonArray bufferViews, byte[] bufferBytes, int accessorIndex) {
        JsonObject accessor = accessors.get(accessorIndex).getAsJsonObject();
        int count = accessor.get("count").getAsInt();
        int componentType = accessor.get("componentType").getAsInt();
        int bufferViewIndex = accessor.get("bufferView").getAsInt();
        int byteOffset = accessor.has("byteOffset") ? accessor.get("byteOffset").getAsInt() : 0;

        JsonObject bufferView = bufferViews.get(bufferViewIndex).getAsJsonObject();
        int viewOffset = bufferView.has("byteOffset") ? bufferView.get("byteOffset").getAsInt() : 0;

        ByteBuffer buffer = ByteBuffer.wrap(bufferBytes).order(ByteOrder.LITTLE_ENDIAN);
        int[] indices = new int[count];
        int base = viewOffset + byteOffset;
        for (int i = 0; i < count; i++) {
            int offset = base + i * componentSize(componentType);
            indices[i] = switch (componentType) {
                case 5123 -> buffer.getShort(offset) & 0xFFFF;
                case 5125 -> buffer.getInt(offset);
                default -> throw new IllegalArgumentException("Unsupported index component type");
            };
        }
        return indices;
    }

    private int componentSize(int componentType) {
        return switch (componentType) {
            case 5123 -> 2;
            case 5125 -> 4;
            default -> throw new IllegalArgumentException("Unsupported component type");
        };
    }

    private float[] defaultNormals(int vertexCount) {
        float[] normals = new float[vertexCount * 3];
        for (int i = 0; i < vertexCount; i++) {
            normals[i * 3] = 0.0f;
            normals[i * 3 + 1] = 1.0f;
            normals[i * 3 + 2] = 0.0f;
        }
        return normals;
    }

    private int[] sequentialIndices(int vertexCount) {
        int[] indices = new int[vertexCount];
        for (int i = 0; i < vertexCount; i++) {
            indices[i] = i;
        }
        return indices;
    }

    private record GltfAsset(JsonObject root, byte[] buffer) {
    }
}
