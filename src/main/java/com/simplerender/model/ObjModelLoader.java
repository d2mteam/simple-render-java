package com.simplerender.model;

import com.simplerender.world.ChunkMeshData;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ObjModelLoader {
    private static final Logger logger = LoggerFactory.getLogger(ObjModelLoader.class);

    private ObjModelLoader() {
    }

    public static ChunkMeshData load(Path path) {
        List<float[]> vertices = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        List<Integer> normalIndices = new ArrayList<>();

        try {
            for (String rawLine : Files.readAllLines(path)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("v ")) {
                    float[] pos = parseVector(line.substring(2));
                    if (pos != null) {
                        vertices.add(pos);
                    }
                } else if (line.startsWith("vn ")) {
                    float[] normal = parseVector(line.substring(3));
                    if (normal != null) {
                        normals.add(normal);
                    }
                } else if (line.startsWith("f ")) {
                    parseFace(line.substring(2), vertices.size(), normals.size(), indices, normalIndices);
                }
            }
        } catch (IOException ex) {
            logger.error("Failed to read OBJ file {}", path, ex);
            return null;
        }

        if (vertices.isEmpty() || indices.isEmpty()) {
            logger.error("OBJ file {} did not contain any geometry", path);
            return null;
        }

        float[] positions = flatten(vertices);
        int[] indexArray = indices.stream().mapToInt(Integer::intValue).toArray();
        float[] resolvedNormals = resolveNormals(vertices.size(), normals, indices, normalIndices);
        logger.info("Loaded OBJ model {} with {} vertices and {} indices", path, vertices.size(), indexArray.length);
        return new ChunkMeshData(vertices.size(), positions, resolvedNormals, indexArray);
    }

    private static float[] parseVector(String value) {
        String[] parts = value.trim().split("\\s+");
        if (parts.length < 3) {
            return null;
        }
        return new float[] {
            Float.parseFloat(parts[0]),
            Float.parseFloat(parts[1]),
            Float.parseFloat(parts[2])
        };
    }

    private static void parseFace(
        String value,
        int vertexCount,
        int normalCount,
        List<Integer> indices,
        List<Integer> normalIndices
    ) {
        String[] parts = value.trim().split("\\s+");
        if (parts.length < 3) {
            return;
        }
        int[] faceVertexIndices = new int[parts.length];
        int[] faceNormalIndices = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String[] element = parts[i].split("/");
            int vertexIndex = parseIndex(element[0], vertexCount);
            int normalIndex = element.length >= 3 && !element[2].isEmpty()
                ? parseIndex(element[2], normalCount)
                : -1;
            faceVertexIndices[i] = vertexIndex;
            faceNormalIndices[i] = normalIndex;
        }
        for (int i = 1; i < faceVertexIndices.length - 1; i++) {
            indices.add(faceVertexIndices[0]);
            indices.add(faceVertexIndices[i]);
            indices.add(faceVertexIndices[i + 1]);

            normalIndices.add(faceNormalIndices[0]);
            normalIndices.add(faceNormalIndices[i]);
            normalIndices.add(faceNormalIndices[i + 1]);
        }
    }

    private static int parseIndex(String token, int size) {
        int index = Integer.parseInt(token);
        if (index < 0) {
            return size + index;
        }
        return index - 1;
    }

    private static float[] flatten(List<float[]> values) {
        float[] flat = new float[values.size() * 3];
        for (int i = 0; i < values.size(); i++) {
            float[] value = values.get(i);
            flat[i * 3] = value[0];
            flat[i * 3 + 1] = value[1];
            flat[i * 3 + 2] = value[2];
        }
        return flat;
    }

    private static float[] resolveNormals(
        int vertexCount,
        List<float[]> normals,
        List<Integer> indices,
        List<Integer> normalIndices
    ) {
        float[] resolved = new float[vertexCount * 3];
        if (normals.isEmpty() || normalIndices.isEmpty()) {
            for (int i = 0; i < vertexCount; i++) {
                resolved[i * 3 + 2] = 1.0f;
            }
            return resolved;
        }
        int count = Math.min(normalIndices.size(), indices.size());
        for (int i = 0; i < count; i++) {
            int normalIndex = normalIndices.get(i);
            if (normalIndex < 0 || normalIndex >= normals.size()) {
                continue;
            }
            float[] normal = normals.get(normalIndex);
            int vertexIndex = indices.get(i);
            if (vertexIndex < 0 || vertexIndex >= vertexCount) {
                continue;
            }
            int offset = vertexIndex * 3;
            resolved[offset] = normal[0];
            resolved[offset + 1] = normal[1];
            resolved[offset + 2] = normal[2];
        }
        return resolved;
    }
}
