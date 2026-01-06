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
            GltfAsset asset;
            try {
                asset = loadAsset(path);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load glTF asset: " + path, e);
            }

            JsonObject root = asset.root();
            byte[][] bufferBytes = asset.buffers();

            JsonArray bufferViews = root.getAsJsonArray("bufferViews");
            JsonArray accessors = root.getAsJsonArray("accessors");
            List<PrimitiveMesh> primitives = readScenePrimitives(root, accessors, bufferViews, bufferBytes);
            List<ImportedPrimitive> importedPrimitives = new ArrayList<>();
            logger.info("Imported {} primitives from {}", primitives.size(), path.getFileName());
            if (!primitives.isEmpty()) {
                GltfMaterialLoader materialLoader = new GltfMaterialLoader(root, bufferBytes, path.getParent());
                for (PrimitiveMesh primitive : primitives) {
                    logger.info("Primitive: vertices={}, radius={}, center={}", primitive.meshData().vertexCount(),
                            primitive.meshData().boundsRadius(), primitive.meshData().boundsCenter());
                    MaterialData material = materialLoader.loadMaterial(primitive.materialIndex());
                    importedPrimitives
                            .add(new ImportedPrimitive(primitive.meshData(), material, primitive.transform()));
                }
            }
            return new ImportedModel(importedPrimitives);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to import glTF: " + path, e);
        }
    }

    private boolean usesDracoCompression(JsonObject root) {
        if (root.has("extensionsRequired")) {
            JsonArray required = root.getAsJsonArray("extensionsRequired");
            for (JsonElement e : required) {
                if ("KHR_draco_mesh_compression".equals(e.getAsString()))
                    return true;
            }
        }
        if (root.has("extensionsUsed")) {
            JsonArray used = root.getAsJsonArray("extensionsUsed");
            for (JsonElement e : used) {
                if ("KHR_draco_mesh_compression".equals(e.getAsString()))
                    return true;
            }
        }
        return false;
    }

    // Recursive processor no longer needed with PreTransformVertices,
    // but we can keep it if we ever want to disable flattening.
    // For now, removing usage to simplify.

    private List<PrimitiveMesh> readScenePrimitives(
            JsonObject root,
            JsonArray accessors,
            JsonArray bufferViews,
            byte[][] bufferBytes) {
        if (!root.has("meshes")) {
            logger.warn("glTF file has no meshes.");
            return List.of();
        }
        int sceneIndex = root.has("scene") ? root.get("scene").getAsInt() : 0;
        JsonArray scenes = root.has("scenes") ? root.getAsJsonArray("scenes") : null;
        JsonArray nodes = root.has("nodes") ? root.getAsJsonArray("nodes") : new JsonArray();
        List<PrimitiveMesh> primitives = new ArrayList<>();

        if (scenes != null && sceneIndex < scenes.size()) {
            logger.info("Loading glTF Scene Index: {}", sceneIndex);
            JsonObject scene = scenes.get(sceneIndex).getAsJsonObject();
            JsonArray rootNodes = scene.has("nodes") ? scene.getAsJsonArray("nodes") : new JsonArray();
            for (int i = 0; i < rootNodes.size(); i++) {
                int nodeIndex = rootNodes.get(i).getAsInt();
                traverseNode(nodeIndex, nodes, root, accessors, bufferViews, bufferBytes, Matrix4f.identity(),
                        primitives);
            }
        } else if (nodes.size() > 0) {
            logger.warn("No 'scene' defined or index out of bounds. Attempting to detect Root Nodes.");
            // Filter nodes that are children of others to find implicit roots.
            java.util.Set<Integer> children = new java.util.HashSet<>();
            for (int i = 0; i < nodes.size(); i++) {
                JsonObject n = nodes.get(i).getAsJsonObject();
                if (n.has("children")) {
                    JsonArray kids = n.getAsJsonArray("children");
                    for (int k = 0; k < kids.size(); k++) {
                        children.add(kids.get(k).getAsInt());
                    }
                }
            }
            int rootsFound = 0;
            for (int i = 0; i < nodes.size(); i++) {
                if (!children.contains(i)) {
                    traverseNode(i, nodes, root, accessors, bufferViews, bufferBytes, Matrix4f.identity(), primitives);
                    rootsFound++;
                }
            }
            logger.info("Found {} implicit root nodes out of {} total nodes.", rootsFound, nodes.size());
        }
        logger.info("Total Primitives Collected: {}", primitives.size());
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
        float[] worldTransform = multiplyMatrices(parentTransform, localTransform);
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

            DracoMesh dracoMesh = null;
            try {
                dracoMesh = decodeDracoPrimitive(primitive, accessors, bufferViews, bufferBytes);
            } catch (Exception e) {
                logger.error("Failed to decode Draco primitive in mesh {} primitive {}", meshIndex, i, e);
                // Continue to next primitive instead of crashing
            }

            // Fallback for non-draco or failed draco
            if (dracoMesh == null && primitive.has("extensions")
                    && primitive.getAsJsonObject("extensions").has("KHR_draco_mesh_compression")) {
                logger.warn("Skipping failed Draco primitive.");
                continue;
            }

            float[] positions = dracoMesh != null
                    ? dracoMesh.positions()
                    : readFloatVec3(accessors, bufferViews, bufferBytes, positionAccessorIndex);

            if (positions == null || positions.length == 0) {
                logger.warn("Primitive has no positions, skipping.");
                continue;
            }

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
                logger.info("Generating normals for primitive {} of mesh {}", i, meshIndex);
                normals = computeNormals(positions, indices);
            }

            // Handle Tangents (read from file or compute)
            float[] tangents = null;
            float[] bitangents = null;

            int tangentAccessorIndex = attributes.has("TANGENT") ? attributes.get("TANGENT").getAsInt() : -1;
            if (tangentAccessorIndex >= 0 && dracoMesh == null) {
                // Read vec4 tangents (x, y, z, w)
                // w indicates handedness of bitangent
                float[] tangentsVec4 = readFloatVec4(accessors, bufferViews, bufferBytes, tangentAccessorIndex);
                if (tangentsVec4 != null && tangentsVec4.length == (positions.length / 3) * 4) {
                    tangents = new float[positions.length]; // xyz
                    bitangents = new float[positions.length]; // xyz
                    // Convert vec4 tangents to vec3 tangents + bitangents
                    for (int v = 0; v < positions.length / 3; v++) {
                        float tx = tangentsVec4[v * 4];
                        float ty = tangentsVec4[v * 4 + 1];
                        float tz = tangentsVec4[v * 4 + 2];
                        float tw = tangentsVec4[v * 4 + 3];

                        float nx = normals[v * 3];
                        float ny = normals[v * 3 + 1];
                        float nz = normals[v * 3 + 2];

                        // Bitangent = Cross(N, T) * w
                        float bx = (ny * tz - nz * ty) * tw;
                        float by = (nz * tx - nx * tz) * tw;
                        float bz = (nx * ty - ny * tx) * tw;

                        tangents[v * 3] = tx;
                        tangents[v * 3 + 1] = ty;
                        tangents[v * 3 + 2] = tz;
                        bitangents[v * 3] = bx;
                        bitangents[v * 3 + 1] = by;
                        bitangents[v * 3 + 2] = bz;
                    }
                }
            }

            // If tangents missing (Draco or Accessor missing), compute them
            if (tangents == null) {
                // Try computing tangents using MikkTSpace-ish logic (or simple logic)
                // We reuse the logic effectively from MeshData but force it here
                // to handle Draco case where MeshData constructor might fail if we passed
                // incomplete data
                // Actually MeshData constructor handles it, but we want to log it
                // Using MeshData's internal computation by passing nulls for explicit tangents
                // But we want to ensure we *have* tangents.
                // For now, let's rely on MeshData constructor's computation for fallback,
                // BUT we must check if UVs are valid.
                // If UVs are missing, MeshData fallback is orthonormal.
                // So passing 'null' to the new constructor is tricky if we want to force
                // computation?
                // No, new constructor with 'null' inputs -> Use compute logic?
                // Wait, the constructor I added creates 'tangents' if they are passed.
                // If I call the OLD constructor (or one without tangents), it computes them.
                // So:
                // If we HAVE explicit tangents from file -> Use new constructor.
                // If we DON'T -> Use old constructor (computes them).

                if (tangentAccessorIndex >= 0 && dracoMesh == null) {
                    logger.warn("Failed to read Tangents from accessor {}", tangentAccessorIndex);
                }
            }

            MeshData meshData;
            if (tangents != null && bitangents != null) {
                meshData = new MeshData(positions, normals, tangents, bitangents, texCoords0, texCoords1, indices);
            } else {
                meshData = new MeshData(positions, normals, texCoords0, texCoords1, indices);
            }

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

        boolean hasTrs = false;
        if (node.has("translation")) {
            JsonArray t = node.getAsJsonArray("translation");
            translation = new float[] { t.get(0).getAsFloat(), t.get(1).getAsFloat(), t.get(2).getAsFloat() };
            hasTrs = true;
        }
        if (node.has("rotation")) {
            JsonArray r = node.getAsJsonArray("rotation");
            rotation = new float[] { r.get(0).getAsFloat(), r.get(1).getAsFloat(), r.get(2).getAsFloat(),
                    r.get(3).getAsFloat() };
            hasTrs = true;
        }
        if (node.has("scale")) {
            JsonArray s = node.getAsJsonArray("scale");
            scale = new float[] { s.get(0).getAsFloat(), s.get(1).getAsFloat(), s.get(2).getAsFloat() };
            hasTrs = true;
        }

        if (hasTrs) {
            // Check for weird scales
            if (scale[0] == 0 || scale[1] == 0 || scale[2] == 0) {
                logger.warn("Node has ZERO scale: {}", java.util.Arrays.toString(scale));
            }
        }

        return composeTransform(translation, rotation, scale);
    }

    private float[] composeTransform(float[] translation, float[] rotation, float[] scale) {
        float x = rotation[0];
        float y = rotation[1];
        float z = rotation[2];
        float w = rotation[3];

        float x2 = x + x, y2 = y + y, z2 = z + z;
        float xx = x * x2, xy = x * y2, xz = x * z2;
        float yy = y * y2, yz = y * z2, zz = z * z2;
        float wx = w * x2, wy = w * y2, wz = w * z2;

        float[] m = Matrix4f.identity();

        // Column 0
        m[0] = (1 - (yy + zz)) * scale[0];
        m[1] = (xy + wz) * scale[0];
        m[2] = (xz - wy) * scale[0];
        m[3] = 0;

        // Column 1
        m[4] = (xy - wz) * scale[1];
        m[5] = (1 - (xx + zz)) * scale[1];
        m[6] = (yz + wx) * scale[1];
        m[7] = 0;

        // Column 2
        m[8] = (xz + wy) * scale[2];
        m[9] = (yz - wx) * scale[2];
        m[10] = (1 - (xx + yy)) * scale[2];
        m[11] = 0;

        // Column 3 (Translation)
        m[12] = translation[0];
        m[13] = translation[1];
        m[14] = translation[2];
        m[15] = 1;

        return m;
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


    private float[] readFloatVec4(JsonArray accessors, JsonArray bufferViews, byte[][] bufferBytes, int accessorIndex) {
        return readFloatVec(accessors, bufferViews, bufferBytes, accessorIndex, 4);
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
        return value / max;
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

    private float[] multiplyMatrices(float[] a, float[] b) {
        float[] result = new float[16];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                float sum = 0.0f;
                for (int k = 0; k < 4; k++) {
                    // Column-major: m[col * 4 + row]
                    // Result(row=j, col=i) = Sum(A(row=j, k) * B(row=k, col=i))
                    // A index: k*4 + j ? No.
                    // Matrix4f uses flat array.
                    // Let's use standard logic:
                    // C = A * B.
                    // C[col][row] = Dot(Row(A, row), Col(B, col))
                    // Row(A, row) elements are at: [row, row+4, row+8, row+12]
                    // Col(B, col) elements are at: [col*4, col*4+1, col*4+2, col*4+3]
                    float aVal = a[k * 4 + j];
                    float bVal = b[i * 4 + k];
                    sum += aVal * bVal;
                }
                result[i * 4 + j] = sum;
            }
        }
        return result;
    }

    record DracoAttributeSpec(int attributeId, int components, int count) {
    }

    private record DracoMesh(float[] positions, float[] normals, float[] texCoords0, float[] texCoords1,
            int[] indices) {
    }
}
