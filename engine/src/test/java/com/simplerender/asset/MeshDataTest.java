package com.simplerender.asset;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MeshDataTest {
    private static final float EPSILON = 1e-5f;

    // A unit quad in the XY plane facing +Z.
    private static final float[] POSITIONS = { 0, 0, 0, 2, 0, 0, 2, 2, 0, 0, 2, 0 };
    private static final float[] NORMALS = { 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1 };
    private static final float[] UVS = { 0, 0, 1, 0, 1, 1, 0, 1 };
    private static final int[] INDICES = { 0, 1, 2, 0, 2, 3 };

    @Test
    void computesBoundingSphere() {
        MeshData mesh = new MeshData(POSITIONS, NORMALS, UVS, INDICES);
        assertArrayEquals(new float[] { 1, 1, 0 },
                new float[] { mesh.boundsCenter().x(), mesh.boundsCenter().y(), mesh.boundsCenter().z() }, EPSILON);
        assertEquals((float) Math.sqrt(2), mesh.boundsRadius(), EPSILON);
    }

    @Test
    void generatesTangentsAlongU() {
        MeshData mesh = new MeshData(POSITIONS, NORMALS, UVS, INDICES);
        float[] tangents = mesh.tangents();
        float[] bitangents = mesh.bitangents();
        for (int v = 0; v < mesh.vertexCount(); v++) {
            assertArrayEquals(new float[] { 1, 0, 0 }, slice(tangents, v), EPSILON, "tangent " + v);
            assertArrayEquals(new float[] { 0, 1, 0 }, slice(bitangents, v), EPSILON, "bitangent " + v);
        }
    }

    @Test
    void missingUvsFallBackToConstantAndSecondSetCopiesFirst() {
        MeshData noUvs = new MeshData(POSITIONS, NORMALS, null, INDICES);
        assertArrayEquals(new float[] { 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f }, noUvs.texCoords0(), EPSILON);

        MeshData oneSet = new MeshData(POSITIONS, NORMALS, UVS, INDICES);
        assertArrayEquals(UVS, oneSet.texCoords1(), EPSILON);
    }

    private static float[] slice(float[] data, int vertex) {
        return new float[] { data[vertex * 3], data[vertex * 3 + 1], data[vertex * 3 + 2] };
    }
}
