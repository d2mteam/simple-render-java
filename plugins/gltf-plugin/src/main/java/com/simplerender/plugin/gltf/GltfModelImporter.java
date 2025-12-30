package com.simplerender.plugin.gltf;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplerender.asset.MaterialData;
import com.simplerender.asset.MeshData;
import com.simplerender.asset.TextureData;
import com.simplerender.asset.plugin.ModelImporter;
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(GltfModelImporter.class);

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
            TextureData textureData = null;
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
                    textureData = loadBaseColorTexture(pbr, root, bufferBytes, path.getParent());
                }
            }

            MeshData meshData = new MeshData(positions, normals, indices);
            MaterialData materialData = new MaterialData(baseColor, textureData);
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

    private TextureData loadBaseColorTexture(
        JsonObject pbr,
        JsonObject root,
        byte[] bufferBytes,
        Path baseDir
    ) {
        if (!pbr.has("baseColorTexture")) {
            return null;
        }
        if (!root.has("textures") || !root.has("images")) {
            logger.warn("glTF texture referenced but textures/images arrays are missing");
            return null;
        }
        int textureIndex = pbr.getAsJsonObject("baseColorTexture").get("index").getAsInt();
        JsonObject texture = root.getAsJsonArray("textures").get(textureIndex).getAsJsonObject();
        if (!texture.has("source")) {
            logger.warn("glTF texture {} has no source index", textureIndex);
            return null;
        }
        int sourceIndex = texture.get("source").getAsInt();
        JsonObject image = root.getAsJsonArray("images").get(sourceIndex).getAsJsonObject();
        try {
            byte[] imageBytes = readImageBytes(image, root, bufferBytes, baseDir);
            if (imageBytes == null) {
                return null;
            }
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (bufferedImage == null) {
                logger.warn("Failed to decode glTF texture image {}", sourceIndex);
                return null;
            }
            logger.info("Loaded glTF texture image {}", sourceIndex);
            return buildTextureData(bufferedImage);
        } catch (Exception e) {
            logger.warn("Failed to load glTF texture image {}", sourceIndex, e);
            return null;
        }
    }

    private byte[] readImageBytes(JsonObject image, JsonObject root, byte[] bufferBytes, Path baseDir)
        throws Exception {
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
        int offset = bufferView.has("byteOffset") ? bufferView.get("byteOffset").getAsInt() : 0;
        int length = bufferView.get("byteLength").getAsInt();
        if (offset + length > bufferBytes.length) {
            throw new IllegalArgumentException("glTF image bufferView exceeds buffer length");
        }
        byte[] imageBytes = new byte[length];
        System.arraycopy(bufferBytes, offset, imageBytes, 0, length);
        return imageBytes;
    }

    private TextureData buildTextureData(BufferedImage image) {
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
