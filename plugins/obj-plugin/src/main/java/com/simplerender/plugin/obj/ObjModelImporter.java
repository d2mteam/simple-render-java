package com.simplerender.plugin.obj;

import com.simplerender.asset.MaterialData;
import com.simplerender.asset.MeshData;
import com.simplerender.asset.TextureData;
import com.simplerender.asset.plugin.ModelImporter;
import com.simplerender.math.Matrix4f;
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
        float[] texCoords = objData.texCoords();
        int[] indices = objData.indices();
        MeshData meshData = new MeshData(positions, normals, texCoords, indices);
        MaterialData materialData = buildMaterial(path, objData.materialLibrary(), objData.materialName());
        return new ImportedModel(List.of(new ImportedPrimitive(meshData, materialData, Matrix4f.identity())));
    }

    private ObjData parseObj(Path path) {
        List<float[]> vertices = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();
        List<float[]> texCoords = new ArrayList<>();
        List<float[]> finalPositions = new ArrayList<>();
        List<float[]> finalNormals = new ArrayList<>();
        List<float[]> finalTexCoords = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
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
                    case "vt" -> texCoords.add(parseVec2(tokens));
                    case "vn" -> normals.add(parseVec3(tokens));
                    case "f" -> {
                        if (tokens.length < 4) {
                            continue;
                        }
                        for (int i = 0; i < 3; i++) {
                            String[] parts = tokens[i + 1].split("/");
                            int vertexIndex = parseIndex(parts, 0) - 1;
                            int texCoordIndex = parseIndex(parts, 1) - 1;
                            int normalIndex = parseIndex(parts, 2) - 1;
                            float[] position = (vertexIndex >= 0 && vertexIndex < vertices.size())
                                ? vertices.get(vertexIndex)
                                : new float[] {0.0f, 0.0f, 0.0f};
                            float[] normal = (normalIndex >= 0 && normalIndex < normals.size())
                                ? normals.get(normalIndex)
                                : new float[] {0.0f, 1.0f, 0.0f};
                            float[] texCoord = (texCoordIndex >= 0 && texCoordIndex < texCoords.size())
                                ? texCoords.get(texCoordIndex)
                                : new float[] {0.5f, 0.5f};
                            finalPositions.add(position);
                            finalNormals.add(normal);
                            finalTexCoords.add(texCoord);
                            indices.add(finalPositions.size() - 1);
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

        float[] positionArray = new float[finalPositions.size() * 3];
        for (int i = 0; i < finalPositions.size(); i++) {
            float[] v = finalPositions.get(i);
            int base = i * 3;
            positionArray[base] = v[0];
            positionArray[base + 1] = v[1];
            positionArray[base + 2] = v[2];
        }
        float[] normalArray = new float[finalNormals.size() * 3];
        for (int i = 0; i < finalNormals.size(); i++) {
            float[] n = finalNormals.get(i);
            int base = i * 3;
            normalArray[base] = n[0];
            normalArray[base + 1] = n[1];
            normalArray[base + 2] = n[2];
        }
        float[] texCoordArray = new float[finalTexCoords.size() * 2];
        for (int i = 0; i < finalTexCoords.size(); i++) {
            float[] t = finalTexCoords.get(i);
            int base = i * 2;
            texCoordArray[base] = t[0];
            texCoordArray[base + 1] = t[1];
        }
        int[] indexArray = indices.stream().mapToInt(Integer::intValue).toArray();
        return new ObjData(positionArray, normalArray, texCoordArray, indexArray, materialLibrary, materialName);
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

    private float[] parseVec2(String[] tokens) {
        return new float[] {
            Float.parseFloat(tokens[1]),
            1.0f - Float.parseFloat(tokens[2])
        };
    }

    private int parseIndex(String[] parts, int index) {
        if (parts.length <= index || parts[index].isBlank()) {
            return 0;
        }
        return Integer.parseInt(parts[index]);
    }

    private record ObjData(
        float[] positions,
        float[] normals,
        float[] texCoords,
        int[] indices,
        String materialLibrary,
        String materialName
    ) {
    }
}
