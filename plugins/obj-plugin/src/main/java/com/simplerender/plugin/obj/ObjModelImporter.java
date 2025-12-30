package com.simplerender.plugin.obj;

import com.simplerender.asset.MaterialData;
import com.simplerender.asset.MeshData;
import com.simplerender.asset.TextureData;
import com.simplerender.asset.plugin.ModelImporter;
import org.pf4j.Extension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Extension
public final class ObjModelImporter implements ModelImporter {
    @Override
    public String[] supportedExtensions() {
        return new String[] {"obj"};
    }

    @Override
    public ImportedModel importModel(Path path) {
        ObjData objData = parseObj(path);
        float[] positions = objData.positions();
        float[] normals = objData.normals();
        int[] indices = objData.indices();
        MeshData meshData = new MeshData(positions, normals, indices);
        MaterialData materialData = buildMaterial(path, objData.materialLibrary(), objData.materialName());
        return new ImportedModel(meshData, materialData);
    }

    private ObjData parseObj(Path path) {
        List<float[]> vertices = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        List<float[]> finalNormals = new ArrayList<>();
        String materialLibrary = null;
        String materialName = null;

        try {
            for (String line : Files.readAllLines(path)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String[] tokens = trimmed.split("\\s+");
                switch (tokens[0]) {
                    case "v" -> vertices.add(parseVec3(tokens));
                    case "vn" -> normals.add(parseVec3(tokens));
                    case "f" -> {
                        if (tokens.length < 4) {
                            continue;
                        }
                        int[] faceIndices = new int[3];
                        float[][] faceNormals = new float[3][];
                        for (int i = 0; i < 3; i++) {
                            String[] parts = tokens[i + 1].split("/");
                            int vertexIndex = Integer.parseInt(parts[0]) - 1;
                            faceIndices[i] = vertexIndex;
                            if (parts.length >= 3 && !parts[2].isEmpty()) {
                                int normalIndex = Integer.parseInt(parts[2]) - 1;
                                if (normalIndex >= 0 && normalIndex < normals.size()) {
                                    faceNormals[i] = normals.get(normalIndex);
                                }
                            }
                        }
                        for (int i = 0; i < 3; i++) {
                            indices.add(faceIndices[i]);
                            finalNormals.add(faceNormals[i]);
                        }
                    }
                    case "mtllib" -> materialLibrary = tokens.length > 1 ? tokens[1] : null;
                    case "usemtl" -> materialName = tokens.length > 1 ? tokens[1] : null;
                    default -> {
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read OBJ: " + path, e);
        }

        float[] positionArray = new float[vertices.size() * 3];
        for (int i = 0; i < vertices.size(); i++) {
            float[] v = vertices.get(i);
            int base = i * 3;
            positionArray[base] = v[0];
            positionArray[base + 1] = v[1];
            positionArray[base + 2] = v[2];
        }
        float[] normalArray = new float[vertices.size() * 3];
        for (int i = 0; i < vertices.size(); i++) {
            int base = i * 3;
            float[] n = (i < finalNormals.size()) ? finalNormals.get(i) : null;
            if (n == null) {
                normalArray[base] = 0.0f;
                normalArray[base + 1] = 1.0f;
                normalArray[base + 2] = 0.0f;
            } else {
                normalArray[base] = n[0];
                normalArray[base + 1] = n[1];
                normalArray[base + 2] = n[2];
            }
        }
        int[] indexArray = indices.stream().mapToInt(Integer::intValue).toArray();
        return new ObjData(positionArray, normalArray, indexArray, materialLibrary, materialName);
    }

    private MaterialData buildMaterial(Path objPath, String materialLibrary, String materialName) {
        if (materialLibrary == null) {
            return new MaterialData(new float[] {0.8f, 0.8f, 0.8f}, null);
        }
        Path mtlPath = objPath.getParent().resolve(materialLibrary);
        float[] baseColor = new float[] {0.8f, 0.8f, 0.8f};
        TextureData textureData = null;
        try {
            String activeMaterial = null;
            for (String line : Files.readAllLines(mtlPath)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String[] tokens = trimmed.split("\\s+");
                switch (tokens[0]) {
                    case "newmtl" -> activeMaterial = tokens.length > 1 ? tokens[1] : null;
                    case "Kd" -> {
                        if (materialName == null || materialName.equals(activeMaterial)) {
                            baseColor = new float[] {
                                Float.parseFloat(tokens[1]),
                                Float.parseFloat(tokens[2]),
                                Float.parseFloat(tokens[3])
                            };
                        }
                    }
                    case "map_Kd" -> {
                        if (materialName == null || materialName.equals(activeMaterial)) {
                            Path texturePath = objPath.getParent().resolve(tokens[1]);
                            textureData = loadTexture(texturePath);
                        }
                    }
                    default -> {
                    }
                }
            }
        } catch (IOException e) {
            return new MaterialData(baseColor, null);
        }
        return new MaterialData(baseColor, textureData);
    }

    private TextureData loadTexture(Path texturePath) {
        try {
            BufferedImage image = ImageIO.read(texturePath.toFile());
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
        } catch (IOException e) {
            return null;
        }
    }

    private float[] parseVec3(String[] tokens) {
        return new float[] {
            Float.parseFloat(tokens[1]),
            Float.parseFloat(tokens[2]),
            Float.parseFloat(tokens[3])
        };
    }

    private record ObjData(float[] positions, float[] normals, int[] indices, String materialLibrary, String materialName) {
    }
}
