package com.simplerender.plugin.gltf;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.simplerender.asset.MaterialData;
import com.simplerender.asset.MeshData;
import com.simplerender.asset.SamplerData;
import com.simplerender.asset.TextureColorSpace;
import com.simplerender.asset.TextureSlot;
import com.simplerender.asset.plugin.ModelImporter.ImportedModel;
import com.simplerender.asset.plugin.ModelImporter.ImportedPrimitive;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GltfModelImporterTest {
    private static final float EPSILON = 1e-5f;

    @TempDir
    Path dir;

    /** Triangle in the XY plane, counter-clockwise seen from +Z. 36 bytes of float positions. */
    private static final float[] TRIANGLE = { 0, 0, 0, 1, 0, 0, 0, 1, 0 };

    @Test
    void generatesNormalsAndIndicesWhenMissing() throws Exception {
        String json = """
                {"asset":{"version":"2.0"},"scenes":[{"nodes":[0]}],"nodes":[{"mesh":0}],
                 "meshes":[{"primitives":[{"attributes":{"POSITION":0}}]}],
                 "buffers":[{"byteLength":36,"uri":"%s"}],
                 "bufferViews":[{"buffer":0,"byteLength":36}],
                 "accessors":[{"bufferView":0,"componentType":5126,"count":3,"type":"VEC3"}]}
                """.formatted(dataUri(floats(TRIANGLE)));
        MeshData mesh = importSingle(json).meshData();
        assertArrayEquals(new int[] { 0, 1, 2 }, mesh.indices());
        assertArrayEquals(new float[] { 0, 0, 1, 0, 0, 1, 0, 0, 1 }, mesh.normals(), EPSILON);
    }

    @Test
    void combinesNodeTransformsDownTheHierarchy() throws Exception {
        // parent: translate (10, 0, 0); child: matrix that scales by 2.
        String json = """
                {"asset":{"version":"2.0"},"scene":0,"scenes":[{"nodes":[0]}],
                 "nodes":[{"translation":[10,0,0],"children":[1]},
                          {"mesh":0,"matrix":[2,0,0,0, 0,2,0,0, 0,0,2,0, 0,0,0,1]}],
                 "meshes":[{"primitives":[{"attributes":{"POSITION":0}}]}],
                 "buffers":[{"byteLength":36,"uri":"%s"}],
                 "bufferViews":[{"buffer":0,"byteLength":36}],
                 "accessors":[{"bufferView":0,"componentType":5126,"count":3,"type":"VEC3"}]}
                """.formatted(dataUri(floats(TRIANGLE)));
        float[] transform = importSingle(json).transform();
        assertArrayEquals(new float[] { 2, 0, 0, 0, 0, 2, 0, 0, 0, 0, 2, 0, 10, 0, 0, 1 }, transform, EPSILON);
    }

    @Test
    void rotationQuaternionBecomesMatrix() throws Exception {
        // 90 degrees about Z: x axis -> y axis.
        String json = """
                {"asset":{"version":"2.0"},"scenes":[{"nodes":[0]}],
                 "nodes":[{"mesh":0,"rotation":[0,0,0.70710678,0.70710678]}],
                 "meshes":[{"primitives":[{"attributes":{"POSITION":0}}]}],
                 "buffers":[{"byteLength":36,"uri":"%s"}],
                 "bufferViews":[{"buffer":0,"byteLength":36}],
                 "accessors":[{"bufferView":0,"componentType":5126,"count":3,"type":"VEC3"}]}
                """.formatted(dataUri(floats(TRIANGLE)));
        float[] m = importSingle(json).transform();
        assertEquals(0.0f, m[0], EPSILON);
        assertEquals(1.0f, m[1], EPSILON);
        assertEquals(-1.0f, m[4], EPSILON);
    }

    @Test
    void readsSparseShortIndicesAndNormalizedByteUvs() throws Exception {
        ByteBuffer bin = ByteBuffer.allocate(36 + 8 + 6 + 2 + 4 + 12).order(ByteOrder.LITTLE_ENDIAN);
        for (float f : TRIANGLE) bin.putFloat(f);                   // 0..36 positions
        bin.put(new byte[] { 0, 0, (byte) 255, 0, 0, (byte) 255, 0, 0 }); // 36..44 uv (3 used, padded)
        bin.putShort((short) 2).putShort((short) 1).putShort((short) 0); // 44..50 indices (ushort)
        bin.putShort((short) 0);                                     // 50..52 padding
        bin.putInt(1);                                               // 52..56 sparse index: vertex 1
        bin.putFloat(5).putFloat(6).putFloat(7);                     // 56..68 sparse value
        String json = """
                {"asset":{"version":"2.0"},"scenes":[{"nodes":[0]}],"nodes":[{"mesh":0}],
                 "meshes":[{"primitives":[{"attributes":{"POSITION":0,"TEXCOORD_0":1},"indices":2}]}],
                 "buffers":[{"byteLength":68,"uri":"%s"}],
                 "bufferViews":[{"buffer":0,"byteLength":36},{"buffer":0,"byteOffset":36,"byteLength":8},
                                {"buffer":0,"byteOffset":44,"byteLength":6},{"buffer":0,"byteOffset":52,"byteLength":4},
                                {"buffer":0,"byteOffset":56,"byteLength":12}],
                 "accessors":[
                   {"bufferView":0,"componentType":5126,"count":3,"type":"VEC3",
                    "sparse":{"count":1,"indices":{"bufferView":3,"componentType":5125},"values":{"bufferView":4}}},
                   {"bufferView":1,"componentType":5121,"normalized":true,"count":3,"type":"VEC2"},
                   {"bufferView":2,"componentType":5123,"count":3,"type":"SCALAR"}]}
                """.formatted(dataUri(bin.array()));
        MeshData mesh = importSingle(json).meshData();
        assertArrayEquals(new int[] { 2, 1, 0 }, mesh.indices());
        assertArrayEquals(new float[] { 0, 0, 0, 5, 6, 7, 0, 1, 0 }, mesh.positions(), EPSILON);
        assertArrayEquals(new float[] { 0, 0, 1, 0, 0, 1 }, mesh.texCoords0(), EPSILON);
    }

    @Test
    void readsBinaryGlb() throws Exception {
        String json = """
                {"asset":{"version":"2.0"},"scenes":[{"nodes":[0]}],"nodes":[{"mesh":0}],
                 "meshes":[{"primitives":[{"attributes":{"POSITION":0}}]}],
                 "buffers":[{"byteLength":36}],
                 "bufferViews":[{"buffer":0,"byteLength":36}],
                 "accessors":[{"bufferView":0,"componentType":5126,"count":3,"type":"VEC3"}]}   \s
                """.strip();
        byte[] jsonBytes = pad(json.getBytes(StandardCharsets.UTF_8), (byte) ' ');
        byte[] binBytes = floats(TRIANGLE);
        ByteBuffer glb = ByteBuffer.allocate(12 + 8 + jsonBytes.length + 8 + binBytes.length)
                .order(ByteOrder.LITTLE_ENDIAN);
        glb.putInt(0x46546C67).putInt(2).putInt(glb.capacity());
        glb.putInt(jsonBytes.length).putInt(0x4E4F534A).put(jsonBytes);
        glb.putInt(binBytes.length).putInt(0x004E4942).put(binBytes);
        Path file = dir.resolve("model.glb");
        Files.write(file, glb.array());

        ImportedModel model = new GltfModelImporter().importModel(file);
        assertEquals(1, model.primitives().size());
        assertArrayEquals(TRIANGLE, model.primitives().get(0).meshData().positions(), EPSILON);
    }

    @Test
    void readsMaterialFactorsTexturesSamplersAndAlpha() throws Exception {
        String json = """
                {"asset":{"version":"2.0"},"scenes":[{"nodes":[0]}],"nodes":[{"mesh":0}],
                 "meshes":[{"primitives":[{"attributes":{"POSITION":0},"material":0}]}],
                 "materials":[{"pbrMetallicRoughness":{"baseColorFactor":[0.5,0.25,1,1],
                                 "baseColorTexture":{"index":0,"texCoord":1}},
                               "normalTexture":{"index":0},
                               "alphaMode":"MASK","alphaCutoff":0.3}],
                 "textures":[{"source":0,"sampler":0}],
                 "samplers":[{"magFilter":9728,"minFilter":9987,"wrapS":33071}],
                 "images":[{"uri":"%s"}],
                 "buffers":[{"byteLength":36,"uri":"%s"}],
                 "bufferViews":[{"buffer":0,"byteLength":36}],
                 "accessors":[{"bufferView":0,"componentType":5126,"count":3,"type":"VEC3"}]}
                """.formatted(pngDataUri(), dataUri(floats(TRIANGLE)));
        MaterialData material = importSingle(json).materialData();
        assertArrayEquals(new float[] { 0.5f, 0.25f, 1 }, material.baseColor(), EPSILON);
        assertEquals(MaterialData.AlphaMode.MASK, material.alphaMode());
        assertEquals(0.3f, material.alphaCutoff(), EPSILON);

        TextureSlot base = material.baseColorTexture().orElseThrow();
        assertEquals(1, base.texCoord());
        assertEquals(TextureColorSpace.SRGB, base.textureData().colorSpace());
        assertEquals(2, base.textureData().width());
        assertEquals(new SamplerData(9987, 9728, 33071, SamplerData.REPEAT), base.samplerData().orElseThrow());
        assertEquals(TextureColorSpace.LINEAR, material.normalTexture().orElseThrow().textureData().colorSpace());
        assertTrue(material.emissiveTexture().isEmpty());
    }

    @Test
    void unlitMaterialShowsItsTextureAsEmission() throws Exception {
        String json = """
                {"asset":{"version":"2.0"},"scenes":[{"nodes":[0]}],"nodes":[{"mesh":0}],
                 "meshes":[{"primitives":[{"attributes":{"POSITION":0},"material":0}]}],
                 "materials":[{"pbrMetallicRoughness":{"baseColorTexture":{"index":0}},
                               "extensions":{"KHR_materials_unlit":{}}}],
                 "textures":[{"source":0}],
                 "images":[{"uri":"%s"}],
                 "buffers":[{"byteLength":36,"uri":"%s"}],
                 "bufferViews":[{"buffer":0,"byteLength":36}],
                 "accessors":[{"bufferView":0,"componentType":5126,"count":3,"type":"VEC3"}]}
                """.formatted(pngDataUri(), dataUri(floats(TRIANGLE)));
        MaterialData material = importSingle(json).materialData();
        assertTrue(material.baseColorTexture().isEmpty());
        assertTrue(material.emissiveTexture().isPresent());
        assertArrayEquals(new float[] { 0, 0, 0 }, material.baseColor(), EPSILON);
    }

    @Test
    void decodesDracoCompressedMeshes() throws Exception {
        // Two quads compressed with gltf-pipeline -d (see scene layout in the file).
        Path file = Path.of(getClass().getResource("/draco/quads-draco.gltf").toURI());
        ImportedModel model = new GltfModelImporter().importModel(file);
        assertEquals(2, model.primitives().size());
        for (ImportedPrimitive primitive : model.primitives()) {
            MeshData mesh = primitive.meshData();
            assertEquals(4, mesh.vertexCount());
            assertEquals(6, mesh.indices().length);
            assertEquals(Math.sqrt(2), mesh.boundsRadius(), 1e-2); // unit quad from -1..1, quantised
        }
    }

    // ---- helpers ----

    private ImportedPrimitive importSingle(String json) throws Exception {
        Path file = dir.resolve("model.gltf");
        Files.writeString(file, json);
        ImportedModel model = new GltfModelImporter().importModel(file);
        assertEquals(1, model.primitives().size());
        return model.primitives().get(0);
    }

    private static byte[] floats(float... values) {
        ByteBuffer buffer = ByteBuffer.allocate(values.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float v : values) {
            buffer.putFloat(v);
        }
        return buffer.array();
    }

    private static String dataUri(byte[] bytes) {
        return "data:application/octet-stream;base64," + Base64.getEncoder().encodeToString(bytes);
    }

    private static String pngDataUri() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xFFFF0000);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private static byte[] pad(byte[] bytes, byte filler) {
        int padded = (bytes.length + 3) / 4 * 4;
        byte[] result = java.util.Arrays.copyOf(bytes, padded);
        java.util.Arrays.fill(result, bytes.length, padded, filler);
        return result;
    }
}
