package com.simplerender.plugin.gltf;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplerender.asset.MaterialData;
import com.simplerender.asset.MeshData;
import com.simplerender.asset.SamplerData;
import com.simplerender.asset.TextureData;
import com.simplerender.asset.TextureSlot;
import com.simplerender.asset.plugin.ModelImporter;
import com.simplerender.math.Matrix4f;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            List<PrimitiveMesh> primitives = readScenePrimitives(root, accessors, bufferViews, bufferBytes);
            List<ImportedPrimitive> importedPrimitives = new ArrayList<>();
            if (!primitives.isEmpty()) {
                Map<Integer, MaterialData> materialCache = new HashMap<>();
                for (PrimitiveMesh primitive : primitives) {
                    MaterialData material = loadMaterialByIndex(
                        primitive.materialIndex(),
                        root,
                        bufferBytes,
                        path.getParent(),
                        materialCache
                    );
                    importedPrimitives.add(new ImportedPrimitive(primitive.meshData(), material));
                }
            }
            return new ImportedModel(importedPrimitives);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to import glTF: " + path, e);
        }
    }

    private List<PrimitiveMesh> readScenePrimitives(
        JsonObject root,
        JsonArray accessors,
        JsonArray bufferViews,
        byte[] bufferBytes
    ) {
        if (!root.has("meshes")) {
            return List.of();
        }
        int sceneIndex = root.has("scene") ? root.get("scene").getAsInt() : 0;
        JsonArray scenes = root.has("scenes") ? root.getAsJsonArray("scenes") : null;
        JsonArray nodes = root.has("nodes") ? root.getAsJsonArray("nodes") : new JsonArray();
        List<PrimitiveMesh> primitives = new ArrayList<>();
        if (scenes != null && sceneIndex < scenes.size()) {
            JsonObject scene = scenes.get(sceneIndex).getAsJsonObject();
            JsonArray rootNodes = scene.has("nodes") ? scene.getAsJsonArray("nodes") : new JsonArray();
            for (int i = 0; i < rootNodes.size(); i++) {
                int nodeIndex = rootNodes.get(i).getAsInt();
                traverseNode(nodeIndex, nodes, root, accessors, bufferViews, bufferBytes, Matrix4f.identity(), primitives);
            }
        } else if (nodes.size() > 0) {
            for (int i = 0; i < nodes.size(); i++) {
                traverseNode(i, nodes, root, accessors, bufferViews, bufferBytes, Matrix4f.identity(), primitives);
            }
        }
        return primitives;
    }

    private void traverseNode(
        int nodeIndex,
        JsonArray nodes,
        JsonObject root,
        JsonArray accessors,
        JsonArray bufferViews,
        byte[] bufferBytes,
        float[] parentTransform,
        List<PrimitiveMesh> primitives
    ) {
        if (nodeIndex < 0 || nodeIndex >= nodes.size()) {
            return;
        }
        JsonObject node = nodes.get(nodeIndex).getAsJsonObject();
        float[] localTransform = readNodeTransform(node);
        float[] worldTransform = Matrix4f.multiply(parentTransform, localTransform);
        if (node.has("mesh")) {
            int meshIndex = node.get("mesh").getAsInt();
            appendMesh(meshIndex, root, accessors, bufferViews, bufferBytes, worldTransform, primitives);
        }
        if (node.has("children")) {
            JsonArray children = node.getAsJsonArray("children");
            for (int i = 0; i < children.size(); i++) {
                int childIndex = children.get(i).getAsInt();
                traverseNode(childIndex, nodes, root, accessors, bufferViews, bufferBytes, worldTransform, primitives);
            }
        }
    }

    private void appendMesh(
        int meshIndex,
        JsonObject root,
        JsonArray accessors,
        JsonArray bufferViews,
        byte[] bufferBytes,
        float[] transform,
        List<PrimitiveMesh> primitives
    ) {
        JsonArray meshes = root.getAsJsonArray("meshes");
        if (meshIndex < 0 || meshIndex >= meshes.size()) {
            return;
        }
        JsonObject mesh = meshes.get(meshIndex).getAsJsonObject();
        JsonArray primitives = mesh.getAsJsonArray("primitives");
        if (primitives == null) {
            return;
        }
        for (int i = 0; i < primitives.size(); i++) {
            JsonObject primitive = primitives.get(i).getAsJsonObject();
            JsonObject attributes = primitive.getAsJsonObject("attributes");
            int positionAccessorIndex = attributes.get("POSITION").getAsInt();
            int normalAccessorIndex = attributes.has("NORMAL") ? attributes.get("NORMAL").getAsInt() : -1;
            int texCoord0AccessorIndex = attributes.has("TEXCOORD_0") ? attributes.get("TEXCOORD_0").getAsInt() : -1;
            int texCoord1AccessorIndex = attributes.has("TEXCOORD_1") ? attributes.get("TEXCOORD_1").getAsInt() : -1;
            int indexAccessorIndex = primitive.has("indices") ? primitive.get("indices").getAsInt() : -1;
            int materialIndex = primitive.has("material") ? primitive.get("material").getAsInt() : -1;
            float[] positions = readFloatVec3(accessors, bufferViews, bufferBytes, positionAccessorIndex);
            float[] normals = normalAccessorIndex >= 0
                ? readFloatVec3(accessors, bufferViews, bufferBytes, normalAccessorIndex)
                : defaultNormals(positions.length / 3);
            float[] texCoords0 = texCoord0AccessorIndex >= 0
                ? readFloatVec2(accessors, bufferViews, bufferBytes, texCoord0AccessorIndex)
                : null;
            float[] texCoords1 = texCoord1AccessorIndex >= 0
                ? readFloatVec2(accessors, bufferViews, bufferBytes, texCoord1AccessorIndex)
                : null;
            applyTransform(positions, normals, transform);
            int[] indices = indexAccessorIndex >= 0
                ? readIndices(accessors, bufferViews, bufferBytes, indexAccessorIndex)
                : sequentialIndices(positions.length / 3);
            MeshData meshData = new MeshData(positions, normals, texCoords0, texCoords1, indices);
            primitives.add(new PrimitiveMesh(meshData, materialIndex));
        }
    }

    private float[] readNodeTransform(JsonObject node) {
        if (node.has("matrix")) {
            JsonArray matrixArray = node.getAsJsonArray("matrix");
            float[] matrix = new float[16];
            for (int i = 0; i < 16; i++) {
                matrix[i] = matrixArray.get(i).getAsFloat();
            }
            return matrix;
        }
        float[] translation = new float[] {0.0f, 0.0f, 0.0f};
        float[] rotation = new float[] {0.0f, 0.0f, 0.0f, 1.0f};
        float[] scale = new float[] {1.0f, 1.0f, 1.0f};
        if (node.has("translation")) {
            JsonArray t = node.getAsJsonArray("translation");
            translation = new float[] {t.get(0).getAsFloat(), t.get(1).getAsFloat(), t.get(2).getAsFloat()};
        }
        if (node.has("rotation")) {
            JsonArray r = node.getAsJsonArray("rotation");
            rotation = new float[] {r.get(0).getAsFloat(), r.get(1).getAsFloat(), r.get(2).getAsFloat(), r.get(3).getAsFloat()};
        }
        if (node.has("scale")) {
            JsonArray s = node.getAsJsonArray("scale");
            scale = new float[] {s.get(0).getAsFloat(), s.get(1).getAsFloat(), s.get(2).getAsFloat()};
        }
        return composeTransform(translation, rotation, scale);
    }

    private float[] composeTransform(float[] translation, float[] rotation, float[] scale) {
        float x = rotation[0];
        float y = rotation[1];
        float z = rotation[2];
        float w = rotation[3];
        float xx = x * x;
        float yy = y * y;
        float zz = z * z;
        float xy = x * y;
        float xz = x * z;
        float yz = y * z;
        float wx = w * x;
        float wy = w * y;
        float wz = w * z;

        float[] matrix = new float[16];
        matrix[0] = (1.0f - 2.0f * (yy + zz)) * scale[0];
        matrix[1] = (2.0f * (xy + wz)) * scale[0];
        matrix[2] = (2.0f * (xz - wy)) * scale[0];
        matrix[4] = (2.0f * (xy - wz)) * scale[1];
        matrix[5] = (1.0f - 2.0f * (xx + zz)) * scale[1];
        matrix[6] = (2.0f * (yz + wx)) * scale[1];
        matrix[8] = (2.0f * (xz + wy)) * scale[2];
        matrix[9] = (2.0f * (yz - wx)) * scale[2];
        matrix[10] = (1.0f - 2.0f * (xx + yy)) * scale[2];
        matrix[12] = translation[0];
        matrix[13] = translation[1];
        matrix[14] = translation[2];
        matrix[15] = 1.0f;
        return matrix;
    }

    private void applyTransform(float[] positions, float[] normals, float[] matrix) {
        for (int i = 0; i < positions.length; i += 3) {
            float x = positions[i];
            float y = positions[i + 1];
            float z = positions[i + 2];
            positions[i] = matrix[0] * x + matrix[4] * y + matrix[8] * z + matrix[12];
            positions[i + 1] = matrix[1] * x + matrix[5] * y + matrix[9] * z + matrix[13];
            positions[i + 2] = matrix[2] * x + matrix[6] * y + matrix[10] * z + matrix[14];
        }
        if (normals == null) {
            return;
        }
        for (int i = 0; i < normals.length; i += 3) {
            float x = normals[i];
            float y = normals[i + 1];
            float z = normals[i + 2];
            float nx = matrix[0] * x + matrix[4] * y + matrix[8] * z;
            float ny = matrix[1] * x + matrix[5] * y + matrix[9] * z;
            float nz = matrix[2] * x + matrix[6] * y + matrix[10] * z;
            float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (length == 0.0f) {
                normals[i] = 0.0f;
                normals[i + 1] = 1.0f;
                normals[i + 2] = 0.0f;
            } else {
                normals[i] = nx / length;
                normals[i + 1] = ny / length;
                normals[i + 2] = nz / length;
            }
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

    private MaterialData loadMaterialByIndex(
        int materialIndex,
        JsonObject root,
        byte[] bufferBytes,
        Path baseDir,
        Map<Integer, MaterialData> materialCache
    ) {
        if (materialIndex >= 0 && materialCache.containsKey(materialIndex)) {
            return materialCache.get(materialIndex);
        }
        if (!root.has("materials") || materialIndex < 0 || materialIndex >= root.getAsJsonArray("materials").size()) {
            return defaultMaterial();
        }
        JsonObject material = root.getAsJsonArray("materials").get(materialIndex).getAsJsonObject();
        float[] baseColor = new float[] {0.8f, 0.8f, 0.8f};
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
                baseColorTexture = loadTextureByIndex(textureIndex, texCoord, root, bufferBytes, baseDir);
            }
            if (pbr.has("metallicRoughnessTexture")) {
                JsonObject textureInfo = pbr.getAsJsonObject("metallicRoughnessTexture");
                int textureIndex = textureInfo.get("index").getAsInt();
                int texCoord = textureInfo.has("texCoord") ? textureInfo.get("texCoord").getAsInt() : 0;
                metallicRoughnessTexture = loadTextureByIndex(textureIndex, texCoord, root, bufferBytes, baseDir);
            }
        }
        if (material.has("normalTexture")) {
            JsonObject textureInfo = material.getAsJsonObject("normalTexture");
            int textureIndex = textureInfo.get("index").getAsInt();
            int texCoord = textureInfo.has("texCoord") ? textureInfo.get("texCoord").getAsInt() : 0;
            normalTexture = loadTextureByIndex(textureIndex, texCoord, root, bufferBytes, baseDir);
        }
        if (material.has("occlusionTexture")) {
            JsonObject textureInfo = material.getAsJsonObject("occlusionTexture");
            int textureIndex = textureInfo.get("index").getAsInt();
            int texCoord = textureInfo.has("texCoord") ? textureInfo.get("texCoord").getAsInt() : 0;
            aoTexture = loadTextureByIndex(textureIndex, texCoord, root, bufferBytes, baseDir);
        }
        if (material.has("emissiveTexture")) {
            JsonObject textureInfo = material.getAsJsonObject("emissiveTexture");
            int textureIndex = textureInfo.get("index").getAsInt();
            int texCoord = textureInfo.has("texCoord") ? textureInfo.get("texCoord").getAsInt() : 0;
            emissiveTexture = loadTextureByIndex(textureIndex, texCoord, root, bufferBytes, baseDir);
        }
        MaterialData materialData = new MaterialData(
            baseColor,
            baseColorTexture,
            normalTexture,
            metallicRoughnessTexture,
            aoTexture,
            emissiveTexture
        );
        materialCache.put(materialIndex, materialData);
        return materialData;
    }

    private MaterialData defaultMaterial() {
        return new MaterialData(
            new float[] {0.8f, 0.8f, 0.8f},
            (TextureSlot) null,
            null,
            null,
            null,
            null
        );
    }

    private TextureSlot loadTextureByIndex(
        int textureIndex,
        int texCoord,
        JsonObject root,
        byte[] bufferBytes,
        Path baseDir
    ) {
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
            samplerData = loadSamplerByIndex(samplerIndex, root);
        }
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
            return new TextureSlot(buildTextureData(bufferedImage), samplerData, texCoord);
        } catch (Exception e) {
            logger.warn("Failed to load glTF texture image {}", sourceIndex, e);
            return null;
        }
    }

    private SamplerData loadSamplerByIndex(int samplerIndex, JsonObject root) {
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
        return readFloatVec(accessors, bufferViews, bufferBytes, accessorIndex, 3);
    }

    private float[] readFloatVec2(JsonArray accessors, JsonArray bufferViews, byte[] bufferBytes, int accessorIndex) {
        return readFloatVec(accessors, bufferViews, bufferBytes, accessorIndex, 2);
    }

    private float[] readFloatVec(
        JsonArray accessors,
        JsonArray bufferViews,
        byte[] bufferBytes,
        int accessorIndex,
        int components
    ) {
        JsonObject accessor = accessors.get(accessorIndex).getAsJsonObject();
        int count = accessor.get("count").getAsInt();
        int componentType = accessor.get("componentType").getAsInt();
        boolean normalized = accessor.has("normalized") && accessor.get("normalized").getAsBoolean();
        int bufferViewIndex = accessor.get("bufferView").getAsInt();
        int byteOffset = accessor.has("byteOffset") ? accessor.get("byteOffset").getAsInt() : 0;

        JsonObject bufferView = bufferViews.get(bufferViewIndex).getAsJsonObject();
        int viewOffset = bufferView.has("byteOffset") ? bufferView.get("byteOffset").getAsInt() : 0;
        int componentSize = componentSize(componentType);
        int stride = bufferView.has("byteStride")
            ? bufferView.get("byteStride").getAsInt()
            : componentSize * components;

        ByteBuffer buffer = ByteBuffer.wrap(bufferBytes).order(ByteOrder.LITTLE_ENDIAN);
        float[] values = new float[count * components];
        for (int i = 0; i < count; i++) {
            int base = viewOffset + byteOffset + i * stride;
            for (int c = 0; c < components; c++) {
                int offset = base + c * componentSize;
                values[i * components + c] = readComponentAsFloat(buffer, offset, componentType, normalized);
            }
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
                case 5121 -> buffer.get(offset) & 0xFF;
                case 5123 -> buffer.getShort(offset) & 0xFFFF;
                case 5125 -> buffer.getInt(offset);
                default -> throw new IllegalArgumentException("Unsupported index component type");
            };
        }
        return indices;
    }

    private int componentSize(int componentType) {
        return switch (componentType) {
            case 5120 -> 1;
            case 5121 -> 1;
            case 5122 -> 2;
            case 5123 -> 2;
            case 5125 -> 4;
            case 5126 -> 4;
            default -> throw new IllegalArgumentException("Unsupported component type");
        };
    }

    private float readComponentAsFloat(ByteBuffer buffer, int offset, int componentType, boolean normalized) {
        return switch (componentType) {
            case 5120 -> normalizeSigned(buffer.get(offset), normalized, 127.0f);
            case 5121 -> normalizeUnsigned(buffer.get(offset) & 0xFF, normalized, 255.0f);
            case 5122 -> normalizeSigned(buffer.getShort(offset), normalized, 32767.0f);
            case 5123 -> normalizeUnsigned(buffer.getShort(offset) & 0xFFFF, normalized, 65535.0f);
            case 5125 -> normalizeUnsigned(Integer.toUnsignedLong(buffer.getInt(offset)), normalized, 4294967295.0f);
            case 5126 -> buffer.getFloat(offset);
            default -> throw new IllegalArgumentException("Unsupported component type");
        };
    }

    private float normalizeSigned(int value, boolean normalized, float max) {
        if (!normalized) {
            return value;
        }
        return Math.max(value / max, -1.0f);
    }

    private float normalizeUnsigned(long value, boolean normalized, float max) {
        if (!normalized) {
            return value;
        }
        return (float) (value / max);
    }

    private static float[] defaultNormals(int vertexCount) {
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

    private record PrimitiveMesh(MeshData meshData, int materialIndex) {
    }

    private record GltfAsset(JsonObject root, byte[] buffer) {
    }
}
