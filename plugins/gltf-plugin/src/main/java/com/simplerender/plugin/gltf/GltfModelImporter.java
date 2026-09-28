package com.simplerender.plugin.gltf;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplerender.asset.MeshData;
import com.simplerender.asset.plugin.ModelImporter;
import com.simplerender.math.Matrix4f;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Imports glTF 2.0 files ({@code .gltf} and {@code .glb}).
 *
 * <p>Walks the node tree of the default scene, turns every mesh primitive into a
 * {@link MeshData} with the node's accumulated world transform, and loads its material.
 * Supports sparse accessors, normalised integer attributes and Draco-compressed meshes.
 */
@Extension
public final class GltfModelImporter implements ModelImporter {
    private static final Logger logger = LoggerFactory.getLogger(GltfModelImporter.class);
    private static final String DRACO = "KHR_draco_mesh_compression";

    /** A primitive found while walking the nodes, before its material is loaded. */
    private record FoundPrimitive(MeshData mesh, int materialIndex, float[] transform) {
    }

    @Override
    public String[] supportedExtensions() {
        return new String[] { "gltf", "glb" };
    }

    @Override
    public ImportedModel importModel(Path path) {
        GltfDocument document;
        try {
            document = GltfDocument.load(path);
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Failed to read glTF file: " + path, e);
        }

        List<FoundPrimitive> found = new ArrayList<>();
        for (int node : rootNodes(document)) {
            visitNode(document, node, Matrix4f.identity(), found);
        }

        GltfMaterialLoader materials = new GltfMaterialLoader(document);
        List<ImportedPrimitive> primitives = new ArrayList<>();
        for (FoundPrimitive primitive : found) {
            primitives.add(new ImportedPrimitive(
                    primitive.mesh(), materials.load(primitive.materialIndex()), primitive.transform()));
        }
        logger.info("Imported {} primitive(s) from {}", primitives.size(), path.getFileName());
        return new ImportedModel(primitives);
    }

    // ---- Scene graph ------------------------------------------------------------------------

    /** The nodes of the default scene, or every node that is nobody's child if there is no scene. */
    private static List<Integer> rootNodes(GltfDocument document) {
        JsonObject root = document.root();
        JsonObject scene = document.object("scenes", GltfDocument.intOr(root, "scene", 0));
        List<Integer> roots = new ArrayList<>();
        if (scene != null) {
            for (JsonElement node : GltfDocument.array(scene, "nodes")) {
                roots.add(node.getAsInt());
            }
            return roots;
        }
        JsonArray nodes = GltfDocument.array(root, "nodes");
        Set<Integer> children = new HashSet<>();
        for (JsonElement node : nodes) {
            for (JsonElement child : GltfDocument.array(node.getAsJsonObject(), "children")) {
                children.add(child.getAsInt());
            }
        }
        for (int i = 0; i < nodes.size(); i++) {
            if (!children.contains(i)) {
                roots.add(i);
            }
        }
        return roots;
    }

    private void visitNode(GltfDocument document, int nodeIndex, float[] parentTransform, List<FoundPrimitive> out) {
        JsonObject node = document.object("nodes", nodeIndex);
        if (node == null) {
            return;
        }
        float[] transform = Matrix4f.multiply(parentTransform, localTransform(node));
        if (node.has("mesh")) {
            JsonObject mesh = document.object("meshes", node.get("mesh").getAsInt());
            JsonArray primitives = mesh == null ? new JsonArray() : GltfDocument.array(mesh, "primitives");
            for (JsonElement primitive : primitives) {
                MeshData data = readPrimitive(document, primitive.getAsJsonObject());
                if (data != null) {
                    int material = GltfDocument.intOr(primitive.getAsJsonObject(), "material", -1);
                    out.add(new FoundPrimitive(data, material, transform));
                }
            }
        }
        for (JsonElement child : GltfDocument.array(node, "children")) {
            visitNode(document, child.getAsInt(), transform, out);
        }
    }

    /** A node's transform: either a {@code matrix} or translation * rotation * scale. */
    private static float[] localTransform(JsonObject node) {
        if (node.has("matrix")) {
            return floats(node.getAsJsonArray("matrix"));
        }
        float[] t = node.has("translation") ? floats(node.getAsJsonArray("translation")) : new float[] { 0, 0, 0 };
        float[] r = node.has("rotation") ? floats(node.getAsJsonArray("rotation")) : new float[] { 0, 0, 0, 1 };
        float[] s = node.has("scale") ? floats(node.getAsJsonArray("scale")) : new float[] { 1, 1, 1 };

        // Rotation quaternion (x, y, z, w) to a 3x3 matrix, each column scaled.
        float x2 = r[0] + r[0];
        float y2 = r[1] + r[1];
        float z2 = r[2] + r[2];
        float xx = r[0] * x2, xy = r[0] * y2, xz = r[0] * z2;
        float yy = r[1] * y2, yz = r[1] * z2, zz = r[2] * z2;
        float wx = r[3] * x2, wy = r[3] * y2, wz = r[3] * z2;
        return new float[] {
                (1 - (yy + zz)) * s[0], (xy + wz) * s[0], (xz - wy) * s[0], 0,
                (xy - wz) * s[1], (1 - (xx + zz)) * s[1], (yz + wx) * s[1], 0,
                (xz + wy) * s[2], (yz - wx) * s[2], (1 - (xx + yy)) * s[2], 0,
                t[0], t[1], t[2], 1,
        };
    }

    // ---- Mesh primitives --------------------------------------------------------------------

    /** Reads one primitive's vertex data, or returns {@code null} if it cannot be used. */
    private MeshData readPrimitive(GltfDocument document, JsonObject primitive) {
        JsonObject attributes = primitive.getAsJsonObject("attributes");
        boolean draco = primitive.has("extensions") && primitive.getAsJsonObject("extensions").has(DRACO);

        float[] positions;
        float[] normals;
        float[] texCoords0;
        float[] texCoords1;
        int[] indices;
        if (draco) {
            DracoDecoder.DecodedDracoMesh decoded;
            try {
                decoded = decodeDraco(document, primitive);
            } catch (RuntimeException | LinkageError e) {
                logger.error("Skipping a Draco-compressed primitive that could not be decoded", e);
                return null;
            }
            if (decoded == null || decoded.attributes().get("POSITION") == null) {
                logger.warn("Skipping a Draco primitive without positions");
                return null;
            }
            positions = decoded.attributes().get("POSITION");
            normals = decoded.attributes().get("NORMAL");
            texCoords0 = decoded.attributes().get("TEXCOORD_0");
            texCoords1 = decoded.attributes().get("TEXCOORD_1");
            indices = decoded.indices();
        } else {
            if (!attributes.has("POSITION")) {
                return null;
            }
            positions = document.readFloats(attributes.get("POSITION").getAsInt(), 3);
            normals = optionalFloats(document, attributes, "NORMAL", 3);
            texCoords0 = optionalFloats(document, attributes, "TEXCOORD_0", 2);
            texCoords1 = optionalFloats(document, attributes, "TEXCOORD_1", 2);
            indices = primitive.has("indices") ? document.readIndices(primitive.get("indices").getAsInt()) : null;
        }
        if (positions.length == 0) {
            return null;
        }

        int vertexCount = positions.length / 3;
        if (indices == null || indices.length == 0) {
            indices = sequentialIndices(vertexCount); // non-indexed: vertices are already in triangle order
        }
        if (!hasUsableNormals(normals, positions.length)) {
            normals = computeNormals(positions, indices);
        }

        float[] tangents = draco ? null : optionalFloats(document, attributes, "TANGENT", 4);
        if (tangents != null && tangents.length == vertexCount * 4) {
            float[][] frame = tangentFrame(tangents, normals);
            return new MeshData(positions, normals, frame[0], frame[1], texCoords0, texCoords1, indices);
        }
        return new MeshData(positions, normals, texCoords0, texCoords1, indices); // tangents are generated
    }

    private static float[] optionalFloats(GltfDocument document, JsonObject attributes, String name, int size) {
        return attributes.has(name) ? document.readFloats(attributes.get(name).getAsInt(), size) : null;
    }

    private DracoDecoder.DecodedDracoMesh decodeDraco(GltfDocument document, JsonObject primitive) {
        JsonObject draco = primitive.getAsJsonObject("extensions").getAsJsonObject(DRACO);
        JsonObject attributes = primitive.getAsJsonObject("attributes");
        Map<String, DracoDecoder.AttributeSpec> specs = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : draco.getAsJsonObject("attributes").entrySet()) {
            if (!attributes.has(entry.getKey())) {
                continue;
            }
            JsonObject accessor = document.object("accessors", attributes.get(entry.getKey()).getAsInt());
            specs.put(entry.getKey(), new DracoDecoder.AttributeSpec(
                    entry.getValue().getAsInt(),
                    GltfDocument.componentCount(accessor),
                    accessor.get("count").getAsInt()));
        }
        byte[] compressed = document.bufferViewBytes(draco.get("bufferView").getAsInt());
        return DracoDecoder.decode(compressed, specs);
    }

    /** glTF tangents are vec4: xyz plus w = +-1 telling which way the bitangent points. */
    private static float[][] tangentFrame(float[] tangents4, float[] normals) {
        int vertexCount = tangents4.length / 4;
        float[] tangents = new float[vertexCount * 3];
        float[] bitangents = new float[vertexCount * 3];
        for (int v = 0; v < vertexCount; v++) {
            float tx = tangents4[v * 4];
            float ty = tangents4[v * 4 + 1];
            float tz = tangents4[v * 4 + 2];
            float w = tangents4[v * 4 + 3];
            float nx = normals[v * 3];
            float ny = normals[v * 3 + 1];
            float nz = normals[v * 3 + 2];
            tangents[v * 3] = tx;
            tangents[v * 3 + 1] = ty;
            tangents[v * 3 + 2] = tz;
            bitangents[v * 3] = (ny * tz - nz * ty) * w; // (N x T) * w
            bitangents[v * 3 + 1] = (nz * tx - nx * tz) * w;
            bitangents[v * 3 + 2] = (nx * ty - ny * tx) * w;
        }
        return new float[][] { tangents, bitangents };
    }

    /** True if there is one normal per vertex and at least one of them is non-zero. */
    private static boolean hasUsableNormals(float[] normals, int expectedLength) {
        if (normals == null || normals.length != expectedLength) {
            return false;
        }
        for (int i = 0; i < normals.length; i += 3) {
            if (normals[i] * normals[i] + normals[i + 1] * normals[i + 1] + normals[i + 2] * normals[i + 2] > 1e-8f) {
                return true;
            }
        }
        return false;
    }

    /** Smooth normals: each vertex gets the normalised sum of its triangles' face normals. */
    private static float[] computeNormals(float[] positions, int[] indices) {
        float[] normals = new float[positions.length];
        for (int i = 0; i + 2 < indices.length; i += 3) {
            int p0 = indices[i] * 3;
            int p1 = indices[i + 1] * 3;
            int p2 = indices[i + 2] * 3;
            if (Math.max(p0, Math.max(p1, p2)) + 2 >= positions.length) {
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
            for (int p : new int[] { p0, p1, p2 }) {
                normals[p] += nx;
                normals[p + 1] += ny;
                normals[p + 2] += nz;
            }
        }
        for (int i = 0; i < normals.length; i += 3) {
            float length = (float) Math.sqrt(normals[i] * normals[i] + normals[i + 1] * normals[i + 1]
                    + normals[i + 2] * normals[i + 2]);
            if (length < 1e-6f) {
                normals[i] = 0.0f;
                normals[i + 1] = 1.0f;
                normals[i + 2] = 0.0f;
            } else {
                normals[i] /= length;
                normals[i + 1] /= length;
                normals[i + 2] /= length;
            }
        }
        return normals;
    }

    private static int[] sequentialIndices(int vertexCount) {
        int[] indices = new int[vertexCount];
        for (int i = 0; i < vertexCount; i++) {
            indices[i] = i;
        }
        return indices;
    }

    private static float[] floats(JsonArray array) {
        float[] values = new float[array.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = array.get(i).getAsFloat();
        }
        return values;
    }
}
