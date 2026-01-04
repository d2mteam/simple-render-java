package com.simplerender.plugin.gltf;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplerender.asset.MaterialData;
import com.simplerender.asset.MeshData;
import com.simplerender.asset.SamplerData;
import com.simplerender.asset.TextureData;
import com.simplerender.asset.TextureColorSpace;
import com.simplerender.asset.TextureSlot;
import com.simplerender.asset.plugin.ModelImporter;
import com.simplerender.math.Matrix4f;
import org.lwjgl.assimp.AIBlob;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.Assimp;
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
        return new String[] { "gltf", "glb" };
    }

    @Override
    public ImportedModel importModel(Path path) {
        try {
            GltfAsset asset = loadAsset(path);
            if (usesDracoCompression(asset.root())) {
                logger.info("Draco-compressed glTF detected. Decoding via Assimp.");
                asset = decodeDracoAsset(path);
            }
            JsonObject root = asset.root();
            byte[][] bufferBytes = asset.buffers();

            JsonArray bufferViews = root.getAsJsonArray("bufferViews");
            JsonArray accessors = root.getAsJsonArray("accessors");
            List<PrimitiveMesh> primitives = readScenePrimitives(root, accessors, bufferViews, bufferBytes);
            List<ImportedPrimitive> importedPrimitives = new ArrayList<>();
            logger.info("Imported {} primitives from {}", primitives.size(), path.getFileName());
            if (!primitives.isEmpty()) {
                Map<Integer, MaterialData> materialCache = new HashMap<>();
                Map<TextureCacheKey, TextureData> textureCache = new HashMap<>();
                for (PrimitiveMesh primitive : primitives) {
                    logger.info("Primitive: vertices={}, radius={}, center={}", primitive.meshData().vertexCount(),
                            primitive.meshData().boundsRadius(), primitive.meshData().boundsCenter());
                    MaterialData material = loadMaterialByIndex(
                            primitive.materialIndex(),
                            root,
                            bufferBytes,
                            path.getParent(),
                            materialCache,
                            textureCache);
                    importedPrimitives
                            .add(new ImportedPrimitive(primitive.meshData(), material, primitive.transform()));
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
            byte[][] bufferBytes) {
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
                traverseNode(nodeIndex, nodes, root, accessors, bufferViews, bufferBytes, Matrix4f.identity(),
                        primitives);
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
            byte[][] bufferBytes,
            float[] parentTransform,
            List<PrimitiveMesh> primitives) {
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
            byte[][] bufferBytes,
            float[] transform,
            List<PrimitiveMesh> primitives) {
        JsonArray meshes = root.getAsJsonArray("meshes");
        if (meshIndex < 0 || meshIndex >= meshes.size()) {
            return;
        }
        JsonObject mesh = meshes.get(meshIndex).getAsJsonObject();
        JsonArray meshPrimitives = mesh.getAsJsonArray("primitives");
        if (meshPrimitives == null) {
            return;
        }
        for (int i = 0; i < meshPrimitives.size(); i++) {
            JsonObject primitive = meshPrimitives.get(i).getAsJsonObject();
            JsonObject attributes = primitive.getAsJsonObject("attributes");
            int positionAccessorIndex = attributes.get("POSITION").getAsInt();
            int normalAccessorIndex = attributes.has("NORMAL") ? attributes.get("NORMAL").getAsInt() : -1;
            int texCoord0AccessorIndex = attributes.has("TEXCOORD_0") ? attributes.get("TEXCOORD_0").getAsInt() : -1;
            int texCoord1AccessorIndex = attributes.has("TEXCOORD_1") ? attributes.get("TEXCOORD_1").getAsInt() : -1;
            int indexAccessorIndex = primitive.has("indices") ? primitive.get("indices").getAsInt() : -1;
            int materialIndex = primitive.has("material") ? primitive.get("material").getAsInt() : -1;
            DracoMesh dracoMesh = decodeDracoPrimitive(primitive, accessors, bufferViews, bufferBytes);
            float[] positions = dracoMesh != null
                    ? dracoMesh.positions()
                    : readFloatVec3(accessors, bufferViews, bufferBytes, positionAccessorIndex);
            float[] texCoords0 = dracoMesh != null
                    ? dracoMesh.texCoords0()
                    : (texCoord0AccessorIndex >= 0
                            ? readFloatVec2(accessors, bufferViews, bufferBytes, texCoord0AccessorIndex)
                            : null);
            float[] texCoords1 = dracoMesh != null
                    ? dracoMesh.texCoords1()
                    : (texCoord1AccessorIndex >= 0
                            ? readFloatVec2(accessors, bufferViews, bufferBytes, texCoord1AccessorIndex)
                            : null);
            int[] indices = dracoMesh != null
                    ? dracoMesh.indices()
                    : (indexAccessorIndex >= 0
                            ? readIndices(accessors, bufferViews, bufferBytes, indexAccessorIndex)
                            : null);
            if (indices == null || indices.length == 0) {
                indices = sequentialIndices(positions.length / 3);
            }
            float[] normals = dracoMesh != null
                    ? dracoMesh.normals()
                    : (normalAccessorIndex >= 0
                            ? readFloatVec3(accessors, bufferViews, bufferBytes, normalAccessorIndex)
                            : null);
            if (!hasValidNormals(normals, positions.length)) {
                normals = computeNormals(positions, indices);
            }
            MeshData meshData = new MeshData(positions, normals, texCoords0, texCoords1, indices);
            primitives.add(new PrimitiveMesh(meshData, materialIndex, transform));
        }
    }

    private DracoMesh decodeDracoPrimitive(
            JsonObject primitive,
            JsonArray accessors,
            JsonArray bufferViews,
            byte[][] bufferBytes) {
        if (!primitive.has("extensions")) {
            return null;
        }
        JsonObject extensions = primitive.getAsJsonObject("extensions");
        if (!extensions.has("KHR_draco_mesh_compression")) {
            return null;
        }
        JsonObject draco = extensions.getAsJsonObject("KHR_draco_mesh_compression");
        int bufferViewIndex = draco.get("bufferView").getAsInt();
        JsonObject bufferView = bufferViews.get(bufferViewIndex).getAsJsonObject();
        byte[] compressed = readBufferViewBytes(bufferView, bufferBytes);
        JsonObject dracoAttributes = draco.getAsJsonObject("attributes");

        Map<String, DracoAttributeSpec> attributeSpecs = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : dracoAttributes.entrySet()) {
            String name = entry.getKey();
            if (!primitive.getAsJsonObject("attributes").has(name)) {
                continue;
            }
            int accessorIndex = primitive.getAsJsonObject("attributes").get(name).getAsInt();
            JsonObject accessor = accessors.get(accessorIndex).getAsJsonObject();
            int components = accessorComponentCount(accessor);
            int count = accessor.get("count").getAsInt();
            int dracoAttributeId = entry.getValue().getAsInt();
            attributeSpecs.put(name, new DracoAttributeSpec(dracoAttributeId, components, count));
        }

        DracoDecoder decoder = DracoDecoder.getInstance();
        DracoDecoder.DecodedDracoMesh decoded = decoder.decode(compressed, attributeSpecs);
        if (decoded == null) {
            return null;
        }
        float[] positions = decoded.attributes().get("POSITION");
        if (positions == null) {
            logger.warn("Draco primitive missing POSITION attribute");
            return null;
        }
        float[] normals = decoded.attributes().get("NORMAL");
        float[] texCoords0 = decoded.attributes().get("TEXCOORD_0");
        float[] texCoords1 = decoded.attributes().get("TEXCOORD_1");
        int vertexCount = positions != null ? positions.length / 3 : decoded.vertexCount();
        int[] indices = decoded.indices();
        if (indices == null || indices.length == 0) {
            indices = sequentialIndices(vertexCount);
        }
        if (!hasValidNormals(normals, vertexCount * 3)) {
            normals = computeNormals(positions, indices);
        }
        return new DracoMesh(positions, normals, texCoords0, texCoords1, indices);
    }

    private int accessorComponentCount(JsonObject accessor) {
        String type = accessor.get("type").getAsString();
        return switch (type) {
            case "SCALAR" -> 1;
            case "VEC2" -> 2;
            case "VEC3" -> 3;
            case "VEC4" -> 4;
            case "MAT2" -> 4;
            case "MAT3" -> 9;
            case "MAT4" -> 16;
            default -> throw new IllegalArgumentException("Unsupported accessor type: " + type);
        };
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
        float[] translation = new float[] { 0.0f, 0.0f, 0.0f };
        float[] rotation = new float[] { 0.0f, 0.0f, 0.0f, 1.0f };
        float[] scale = new float[] { 1.0f, 1.0f, 1.0f };
        if (node.has("translation")) {
            JsonArray t = node.getAsJsonArray("translation");
            translation = new float[] { t.get(0).getAsFloat(), t.get(1).getAsFloat(), t.get(2).getAsFloat() };
        }
        if (node.has("rotation")) {
            JsonArray r = node.getAsJsonArray("rotation");
            rotation = new float[] { r.get(0).getAsFloat(), r.get(1).getAsFloat(), r.get(2).getAsFloat(),
                    r.get(3).getAsFloat() };
        }
        if (node.has("scale")) {
            JsonArray s = node.getAsJsonArray("scale");
            scale = new float[] { s.get(0).getAsFloat(), s.get(1).getAsFloat(), s.get(2).getAsFloat() };
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

        float[] rotationMatrix = Matrix4f.identity();
        rotationMatrix[0] = 1.0f - 2.0f * (yy + zz);
        rotationMatrix[1] = 2.0f * (xy + wz);
        rotationMatrix[2] = 2.0f * (xz - wy);
        rotationMatrix[4] = 2.0f * (xy - wz);
        rotationMatrix[5] = 1.0f - 2.0f * (xx + zz);
        rotationMatrix[6] = 2.0f * (yz + wx);
        rotationMatrix[8] = 2.0f * (xz + wy);
        rotationMatrix[9] = 2.0f * (yz - wx);
        rotationMatrix[10] = 1.0f - 2.0f * (xx + yy);

        float[] scaleMatrix = Matrix4f.identity();
        scaleMatrix[0] = scale[0];
        scaleMatrix[5] = scale[1];
        scaleMatrix[10] = scale[2];

        float[] rotationScale = Matrix4f.multiply(rotationMatrix, scaleMatrix);
        float[] translationMatrix = Matrix4f.identity();
        translationMatrix[12] = translation[0];
        translationMatrix[13] = translation[1];
        translationMatrix[14] = translation[2];
        return Matrix4f.multiply(translationMatrix, rotationScale);
    }

    private GltfAsset loadAsset(Path path) throws Exception {
        if (path.toString().toLowerCase().endsWith(".glb")) {
            return loadGlb(path);
        }
        String json = Files.readString(path);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        byte[][] bufferBytes = readBuffers(root, path.getParent());
        return new GltfAsset(root, bufferBytes);
    }

    private GltfAsset loadGlb(Path path) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        return loadGlbBytes(bytes, path.getParent());
    }

    private GltfAsset loadGlbBytes(byte[] bytes, Path baseDir) throws Exception {
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
        Path resolvedBase = baseDir == null ? Path.of(".") : baseDir;
        byte[][] bufferBytes = readBuffers(root, resolvedBase);
        if (bufferBytes.length == 0) {
            bufferBytes = new byte[][] { binBytes };
        } else {
            bufferBytes[0] = binBytes;
        }
        return new GltfAsset(root, bufferBytes);
    }

    private boolean usesDracoCompression(JsonObject root) {
        if (root.has("extensionsUsed")) {
            JsonArray extensionsUsed = root.getAsJsonArray("extensionsUsed");
            for (int i = 0; i < extensionsUsed.size(); i++) {
                if ("KHR_draco_mesh_compression".equals(extensionsUsed.get(i).getAsString())) {
                    return true;
                }
            }
        }
        if (!root.has("meshes")) {
            return false;
        }
        JsonArray meshes = root.getAsJsonArray("meshes");
        for (int i = 0; i < meshes.size(); i++) {
            JsonObject mesh = meshes.get(i).getAsJsonObject();
            JsonArray primitives = mesh.getAsJsonArray("primitives");
            if (primitives == null) {
                continue;
            }
            for (int p = 0; p < primitives.size(); p++) {
                JsonObject primitive = primitives.get(p).getAsJsonObject();
                if (primitive.has("extensions")) {
                    JsonObject extensions = primitive.getAsJsonObject("extensions");
                    if (extensions.has("KHR_draco_mesh_compression")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private GltfAsset decodeDracoAsset(Path path) throws Exception {
        int flags = Assimp.aiProcess_Triangulate
                | Assimp.aiProcess_JoinIdenticalVertices
                | Assimp.aiProcess_GenNormals;
        AIScene scene = Assimp.aiImportFile(path.toString(), flags);
        if (scene == null) {
            throw new IllegalStateException("Assimp failed to import Draco glTF: " + Assimp.aiGetErrorString());
        }
        AIBlob blob = Assimp.aiExportSceneToBlob(scene, "glb2", 0);
        Assimp.aiReleaseImport(scene);
        if (blob == null) {
            throw new IllegalStateException("Assimp failed to export decoded GLB: " + Assimp.aiGetErrorString());
        }
        byte[] decodedBytes = new byte[blob.size()];
        ByteBuffer blobData = blob.data();
        blobData.rewind();
        blobData.get(decodedBytes);
        Assimp.aiReleaseExportBlob(blob);
        return loadGlbBytes(decodedBytes, path.getParent());
    }

    private byte[] decodeUri(String uri, Path base) throws Exception {
        if (uri.startsWith("data:")) {
            String base64 = uri.substring(uri.indexOf(',') + 1);
            return Base64.getDecoder().decode(base64);
        }
        return Files.readAllBytes(base.resolve(uri));
    }

    private byte[][] readBuffers(JsonObject root, Path baseDir) throws Exception {
        if (!root.has("buffers")) {
            return new byte[0][];
        }
        JsonArray buffers = root.getAsJsonArray("buffers");
        byte[][] bufferBytes = new byte[buffers.size()][];
        for (int i = 0; i < buffers.size(); i++) {
            JsonObject buffer = buffers.get(i).getAsJsonObject();
            if (buffer.has("uri")) {
                bufferBytes[i] = decodeUri(buffer.get("uri").getAsString(), baseDir);
            }
        }
        return bufferBytes;
    }

    private MaterialData loadMaterialByIndex(
            int materialIndex,
            JsonObject root,
            byte[][] bufferBytes,
            Path baseDir,
            Map<Integer, MaterialData> materialCache,
            Map<TextureCacheKey, TextureData> textureCache) {
        if (materialIndex >= 0 && materialCache.containsKey(materialIndex)) {
            return materialCache.get(materialIndex);
        }
        if (!root.has("materials") || materialIndex < 0 || materialIndex >= root.getAsJsonArray("materials").size()) {
            return defaultMaterial();
        }
        JsonObject material = root.getAsJsonArray("materials").get(materialIndex).getAsJsonObject();
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
                        TextureColorSpace.SRGB,
                        root,
                        bufferBytes,
                        baseDir,
                        textureCache);
            }
            if (pbr.has("metallicRoughnessTexture")) {
                JsonObject textureInfo = pbr.getAsJsonObject("metallicRoughnessTexture");
                int textureIndex = textureInfo.get("index").getAsInt();
                int texCoord = textureInfo.has("texCoord") ? textureInfo.get("texCoord").getAsInt() : 0;
                metallicRoughnessTexture = loadTextureByIndex(
                        textureIndex,
                        texCoord,
                        TextureColorSpace.LINEAR,
                        root,
                        bufferBytes,
                        baseDir,
                        textureCache);
            }
        }
        if (material.has("normalTexture")) {
            JsonObject textureInfo = material.getAsJsonObject("normalTexture");
            int textureIndex = textureInfo.get("index").getAsInt();
            int texCoord = textureInfo.has("texCoord") ? textureInfo.get("texCoord").getAsInt() : 0;
            normalTexture = loadTextureByIndex(
                    textureIndex,
                    texCoord,
                    TextureColorSpace.LINEAR,
                    root,
                    bufferBytes,
                    baseDir,
                    textureCache);
        }
        if (material.has("occlusionTexture")) {
            JsonObject textureInfo = material.getAsJsonObject("occlusionTexture");
            int textureIndex = textureInfo.get("index").getAsInt();
            int texCoord = textureInfo.has("texCoord") ? textureInfo.get("texCoord").getAsInt() : 0;
            aoTexture = loadTextureByIndex(
                    textureIndex,
                    texCoord,
                    TextureColorSpace.LINEAR,
                    root,
                    bufferBytes,
                    baseDir,
                    textureCache);
        }
        if (material.has("emissiveTexture")) {
            JsonObject textureInfo = material.getAsJsonObject("emissiveTexture");
            int textureIndex = textureInfo.get("index").getAsInt();
            int texCoord = textureInfo.has("texCoord") ? textureInfo.get("texCoord").getAsInt() : 0;
            emissiveTexture = loadTextureByIndex(
                    textureIndex,
                    texCoord,
                    TextureColorSpace.SRGB,
                    root,
                    bufferBytes,
                    baseDir,
                    textureCache);
        }
        MaterialData materialData = new MaterialData(
                baseColor,
                baseColorTexture,
                normalTexture,
                metallicRoughnessTexture,
                aoTexture,
                emissiveTexture);
        materialCache.put(materialIndex, materialData);
        return materialData;
    }

    private MaterialData defaultMaterial() {
        return new MaterialData(
                new float[] { 0.8f, 0.8f, 0.8f },
                (TextureSlot) null,
                null,
                null,
                null,
                null);
    }

    private TextureSlot loadTextureByIndex(
            int textureIndex,
            int texCoord,
            TextureColorSpace colorSpace,
            JsonObject root,
            byte[][] bufferBytes,
            Path baseDir,
            Map<TextureCacheKey, TextureData> textureCache) {
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
        TextureCacheKey cacheKey = new TextureCacheKey(sourceIndex, colorSpace);
        if (textureCache.containsKey(cacheKey)) {
            return new TextureSlot(textureCache.get(cacheKey), samplerData, texCoord);
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
            logger.info("Loaded glTF texture image {} ({}x{})", sourceIndex, bufferedImage.getWidth(),
                    bufferedImage.getHeight());
            // Debug: Dump texture to disk
            try {
                javax.imageio.ImageIO.write(bufferedImage, "png",
                        new java.io.File("debug_texture_" + sourceIndex + ".png"));
            } catch (Exception e) {
                // ignore
            }
            TextureData textureData = buildTextureData(bufferedImage, colorSpace);
            textureCache.put(cacheKey, textureData);
            return new TextureSlot(textureData, samplerData, texCoord);
        } catch (Exception e) {
            logger.warn("Failed to load glTF texture image {}", sourceIndex, e);
            return null;
        }
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

    private byte[] readImageBytes(JsonObject image, JsonObject root, byte[][] bufferBytes, Path baseDir)
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

    private float[] readFloatVec3(JsonArray accessors, JsonArray bufferViews, byte[][] bufferBytes, int accessorIndex) {
        return readFloatVec(accessors, bufferViews, bufferBytes, accessorIndex, 3);
    }

    private float[] readFloatVec2(JsonArray accessors, JsonArray bufferViews, byte[][] bufferBytes, int accessorIndex) {
        return readFloatVec(accessors, bufferViews, bufferBytes, accessorIndex, 2);
    }

    private float[] readFloatVec(
            JsonArray accessors,
            JsonArray bufferViews,
            byte[][] bufferBytes,
            int accessorIndex,
            int components) {
        JsonObject accessor = accessors.get(accessorIndex).getAsJsonObject();
        int count = accessor.get("count").getAsInt();
        int componentType = accessor.get("componentType").getAsInt();
        boolean normalized = accessor.has("normalized") && accessor.get("normalized").getAsBoolean();

        float[] values = readFloatAccessorData(accessor, bufferViews, bufferBytes, count, componentType, normalized,
                components);
        if (accessor.has("sparse")) {
            applySparseFloats(accessor.getAsJsonObject("sparse"), values, componentType, normalized, components,
                    bufferViews, bufferBytes);
        }
        return values;
    }

    private int[] readIndices(JsonArray accessors, JsonArray bufferViews, byte[][] bufferBytes, int accessorIndex) {
        JsonObject accessor = accessors.get(accessorIndex).getAsJsonObject();
        int count = accessor.get("count").getAsInt();
        int componentType = accessor.get("componentType").getAsInt();

        int[] indices = readIndicesAccessorData(accessor, bufferViews, bufferBytes, count, componentType);
        if (accessor.has("sparse")) {
            applySparseIndices(accessor.getAsJsonObject("sparse"), indices, componentType, bufferViews, bufferBytes);
        }
        return indices;
    }

    private float[] readFloatAccessorData(
            JsonObject accessor,
            JsonArray bufferViews,
            byte[][] bufferBytes,
            int count,
            int componentType,
            boolean normalized,
            int components) {
        if (!accessor.has("bufferView")) {
            return new float[count * components];
        }

        int bufferViewIndex = accessor.get("bufferView").getAsInt();
        int byteOffset = accessor.has("byteOffset") ? accessor.get("byteOffset").getAsInt() : 0;

        JsonObject bufferView = bufferViews.get(bufferViewIndex).getAsJsonObject();
        int viewOffset = bufferView.has("byteOffset") ? bufferView.get("byteOffset").getAsInt() : 0;
        int componentSize = componentSize(componentType);
        int stride = bufferView.has("byteStride")
                ? bufferView.get("byteStride").getAsInt()
                : componentSize * components;

        ByteBuffer buffer = ByteBuffer.wrap(bufferBytesForView(bufferView, bufferBytes)).order(ByteOrder.LITTLE_ENDIAN);
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

    private void applySparseFloats(
            JsonObject sparse,
            float[] values,
            int componentType,
            boolean normalized,
            int components,
            JsonArray bufferViews,
            byte[][] bufferBytes) {
        int sparseCount = sparse.get("count").getAsInt();
        JsonObject indices = sparse.getAsJsonObject("indices");
        JsonObject sparseValues = sparse.getAsJsonObject("values");

        ByteBuffer indicesBuffer = bufferForView(indices, bufferViews, bufferBytes);
        ByteBuffer valuesBuffer = bufferForView(sparseValues, bufferViews, bufferBytes);
        int indexComponentType = indices.get("componentType").getAsInt();
        int indexComponentSize = componentSize(indexComponentType);
        int valueComponentSize = componentSize(componentType);
        int indicesOffset = indices.has("byteOffset") ? indices.get("byteOffset").getAsInt() : 0;
        int valuesOffset = sparseValues.has("byteOffset") ? sparseValues.get("byteOffset").getAsInt() : 0;

        for (int i = 0; i < sparseCount; i++) {
            int indexOffset = indicesOffset + i * indexComponentSize;
            int vertexIndex = readIndexComponent(indicesBuffer, indexOffset, indexComponentType);
            int valueBase = valuesOffset + i * valueComponentSize * components;
            int destBase = vertexIndex * components;
            for (int c = 0; c < components; c++) {
                int valueOffset = valueBase + c * valueComponentSize;
                values[destBase + c] = readComponentAsFloat(valuesBuffer, valueOffset, componentType, normalized);
            }
        }
    }

    private int[] readIndicesAccessorData(
            JsonObject accessor,
            JsonArray bufferViews,
            byte[][] bufferBytes,
            int count,
            int componentType) {
        if (!accessor.has("bufferView")) {
            return new int[count];
        }

        int bufferViewIndex = accessor.get("bufferView").getAsInt();
        int byteOffset = accessor.has("byteOffset") ? accessor.get("byteOffset").getAsInt() : 0;

        JsonObject bufferView = bufferViews.get(bufferViewIndex).getAsJsonObject();
        int viewOffset = bufferView.has("byteOffset") ? bufferView.get("byteOffset").getAsInt() : 0;

        ByteBuffer buffer = ByteBuffer.wrap(bufferBytesForView(bufferView, bufferBytes)).order(ByteOrder.LITTLE_ENDIAN);
        int[] indices = new int[count];
        int base = viewOffset + byteOffset;
        int componentSize = componentSize(componentType);
        for (int i = 0; i < count; i++) {
            int offset = base + i * componentSize;
            indices[i] = switch (componentType) {
                case 5121 -> buffer.get(offset) & 0xFF;
                case 5123 -> buffer.getShort(offset) & 0xFFFF;
                case 5125 -> buffer.getInt(offset);
                default -> throw new IllegalArgumentException("Unsupported index component type");
            };
        }
        return indices;
    }

    private void applySparseIndices(
            JsonObject sparse,
            int[] indices,
            int componentType,
            JsonArray bufferViews,
            byte[][] bufferBytes) {
        int sparseCount = sparse.get("count").getAsInt();
        JsonObject sparseIndices = sparse.getAsJsonObject("indices");
        JsonObject sparseValues = sparse.getAsJsonObject("values");
        int indexComponentType = sparseIndices.get("componentType").getAsInt();
        int indexComponentSize = componentSize(indexComponentType);
        int valueComponentSize = componentSize(componentType);
        int indicesOffset = sparseIndices.has("byteOffset") ? sparseIndices.get("byteOffset").getAsInt() : 0;
        int valuesOffset = sparseValues.has("byteOffset") ? sparseValues.get("byteOffset").getAsInt() : 0;
        ByteBuffer indicesBuffer = bufferForView(sparseIndices, bufferViews, bufferBytes);
        ByteBuffer valuesBuffer = bufferForView(sparseValues, bufferViews, bufferBytes);

        for (int i = 0; i < sparseCount; i++) {
            int indexOffset = indicesOffset + i * indexComponentSize;
            int accessorIndex = readIndexComponent(indicesBuffer, indexOffset, indexComponentType);
            int valueOffset = valuesOffset + i * valueComponentSize;
            indices[accessorIndex] = readIndexComponent(valuesBuffer, valueOffset, componentType);
        }
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

    private int readIndexComponent(ByteBuffer buffer, int offset, int componentType) {
        return switch (componentType) {
            case 5121 -> buffer.get(offset) & 0xFF;
            case 5123 -> buffer.getShort(offset) & 0xFFFF;
            case 5125 -> buffer.getInt(offset);
            default -> throw new IllegalArgumentException("Unsupported index component type");
        };
    }

    private ByteBuffer bufferForView(JsonObject viewOwner, JsonArray bufferViews, byte[][] bufferBytes) {
        int bufferViewIndex = viewOwner.get("bufferView").getAsInt();
        JsonObject bufferView = bufferViews.get(bufferViewIndex).getAsJsonObject();
        int viewOffset = bufferView.has("byteOffset") ? bufferView.get("byteOffset").getAsInt() : 0;
        ByteBuffer buffer = ByteBuffer.wrap(bufferBytesForView(bufferView, bufferBytes)).order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(viewOffset);
        return buffer.slice().order(ByteOrder.LITTLE_ENDIAN);
    }

    private byte[] bufferBytesForView(JsonObject bufferView, byte[][] bufferBytes) {
        int bufferIndex = bufferView.has("buffer") ? bufferView.get("buffer").getAsInt() : 0;
        if (bufferIndex < 0 || bufferIndex >= bufferBytes.length || bufferBytes[bufferIndex] == null) {
            throw new IllegalArgumentException("glTF buffer index out of range: " + bufferIndex);
        }
        return bufferBytes[bufferIndex];
    }

    private byte[] readBufferViewBytes(JsonObject bufferView, byte[][] bufferBytes) {
        byte[] source = bufferBytesForView(bufferView, bufferBytes);
        int offset = bufferView.has("byteOffset") ? bufferView.get("byteOffset").getAsInt() : 0;
        int length = bufferView.get("byteLength").getAsInt();
        if (offset + length > source.length) {
            throw new IllegalArgumentException("glTF bufferView exceeds buffer length");
        }
        byte[] slice = new byte[length];
        System.arraycopy(source, offset, slice, 0, length);
        return slice;
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

    private static boolean hasValidNormals(float[] normals, int expectedLength) {
        if (normals == null || normals.length != expectedLength) {
            return false;
        }
        for (int i = 0; i < normals.length; i += 3) {
            float nx = normals[i];
            float ny = normals[i + 1];
            float nz = normals[i + 2];
            if (nx * nx + ny * ny + nz * nz > 1e-8f) {
                return true;
            }
        }
        return false;
    }

    private static float[] computeNormals(float[] positions, int[] indices) {
        int vertexCount = positions.length / 3;
        if (vertexCount == 0) {
            return defaultNormals(0);
        }
        float[] normals = new float[vertexCount * 3];
        for (int i = 0; i + 2 < indices.length; i += 3) {
            int i0 = indices[i];
            int i1 = indices[i + 1];
            int i2 = indices[i + 2];
            int p0 = i0 * 3;
            int p1 = i1 * 3;
            int p2 = i2 * 3;
            if (p2 + 2 >= positions.length || p1 + 2 >= positions.length || p0 + 2 >= positions.length) {
                continue;
            }
            float x1 = positions[p1] - positions[p0];
            float y1 = positions[p1 + 1] - positions[p0 + 1];
            float z1 = positions[p1 + 2] - positions[p0 + 2];
            float x2 = positions[p2] - positions[p0];
            float y2 = positions[p2 + 1] - positions[p0 + 1];
            float z2 = positions[p2 + 2] - positions[p0 + 2];
            float nx = y1 * z2 - z1 * y2;
            float ny = z1 * x2 - x1 * z2;
            float nz = x1 * y2 - y1 * x2;
            accumulate(normals, i0, nx, ny, nz);
            accumulate(normals, i1, nx, ny, nz);
            accumulate(normals, i2, nx, ny, nz);
        }
        for (int i = 0; i < vertexCount; i++) {
            int base = i * 3;
            float nx = normals[base];
            float ny = normals[base + 1];
            float nz = normals[base + 2];
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len < 1e-6f) {
                normals[base] = 0.0f;
                normals[base + 1] = 1.0f;
                normals[base + 2] = 0.0f;
            } else {
                normals[base] = nx / len;
                normals[base + 1] = ny / len;
                normals[base + 2] = nz / len;
            }
        }
        return normals;
    }

    private static void accumulate(float[] data, int index, float x, float y, float z) {
        int base = index * 3;
        if (base + 2 >= data.length) {
            return;
        }
        data[base] += x;
        data[base + 1] += y;
        data[base + 2] += z;
    }

    private int[] sequentialIndices(int vertexCount) {
        int[] indices = new int[vertexCount];
        for (int i = 0; i < vertexCount; i++) {
            indices[i] = i;
        }
        return indices;
    }

    private record PrimitiveMesh(MeshData meshData, int materialIndex, float[] transform) {
    }

    private record GltfAsset(JsonObject root, byte[][] buffers) {
    }

    record DracoAttributeSpec(int attributeId, int components, int count) {
    }

    private record DracoMesh(float[] positions, float[] normals, float[] texCoords0, float[] texCoords1, int[] indices) {
    }
}
