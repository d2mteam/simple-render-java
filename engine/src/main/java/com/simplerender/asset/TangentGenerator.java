package com.simplerender.asset;

/**
 * Computes per-vertex tangents and bitangents for normal mapping.
 *
 * <p>Tangents are accumulated per triangle from the UV gradients, then Gram-Schmidt
 * orthogonalised against the vertex normal. Vertices without usable UVs get an arbitrary
 * tangent frame that is still perpendicular to the normal.
 */
final class TangentGenerator {
    record Tangents(float[] tangents, float[] bitangents) {
    }

    private TangentGenerator() {
    }

    static Tangents generate(float[] positions, float[] normals, float[] texCoords, int[] indices) {
        int vertexCount = positions.length / 3;
        float[] tangents = new float[vertexCount * 3];
        float[] bitangents = new float[vertexCount * 3];
        if (indices.length < 3 || texCoords.length != vertexCount * 2) {
            fillFallbackTangents(normals, tangents, bitangents);
            return new Tangents(tangents, bitangents);
        }
        accumulateTriangleTangents(positions, texCoords, indices, tangents, bitangents);
        orthonormalize(normals, tangents, bitangents);
        return new Tangents(tangents, bitangents);
    }

    private static void accumulateTriangleTangents(
            float[] positions,
            float[] texCoords,
            int[] indices,
            float[] tangents,
            float[] bitangents) {
        for (int i = 0; i < indices.length; i += 3) {
            int i0 = indices[i];
            int i1 = indices[i + 1];
            int i2 = indices[i + 2];
            int p0 = i0 * 3;
            int p1 = i1 * 3;
            int p2 = i2 * 3;
            int uv0 = i0 * 2;
            int uv1 = i1 * 2;
            int uv2 = i2 * 2;
            float x1 = positions[p1] - positions[p0];
            float y1 = positions[p1 + 1] - positions[p0 + 1];
            float z1 = positions[p1 + 2] - positions[p0 + 2];
            float x2 = positions[p2] - positions[p0];
            float y2 = positions[p2 + 1] - positions[p0 + 1];
            float z2 = positions[p2 + 2] - positions[p0 + 2];
            float s1 = texCoords[uv1] - texCoords[uv0];
            float t1 = texCoords[uv1 + 1] - texCoords[uv0 + 1];
            float s2 = texCoords[uv2] - texCoords[uv0];
            float t2 = texCoords[uv2 + 1] - texCoords[uv0 + 1];
            float denom = s1 * t2 - s2 * t1;
            if (Math.abs(denom) < 1e-8f) {
                continue;
            }
            float r = 1.0f / denom;
            float tx = (x1 * t2 - x2 * t1) * r;
            float ty = (y1 * t2 - y2 * t1) * r;
            float tz = (z1 * t2 - z2 * t1) * r;
            float bx = (x2 * s1 - x1 * s2) * r;
            float by = (y2 * s1 - y1 * s2) * r;
            float bz = (z2 * s1 - z1 * s2) * r;
            add(tangents, i0, tx, ty, tz);
            add(tangents, i1, tx, ty, tz);
            add(tangents, i2, tx, ty, tz);
            add(bitangents, i0, bx, by, bz);
            add(bitangents, i1, bx, by, bz);
            add(bitangents, i2, bx, by, bz);
        }
    }

    /** Makes each tangent perpendicular to its normal and rebuilds the bitangent as N x T. */
    private static void orthonormalize(float[] normals, float[] tangents, float[] bitangents) {
        int vertexCount = tangents.length / 3;
        for (int i = 0; i < vertexCount; i++) {
            int base = i * 3;
            float nx = normals[base];
            float ny = normals[base + 1];
            float nz = normals[base + 2];
            float tx = tangents[base];
            float ty = tangents[base + 1];
            float tz = tangents[base + 2];
            float dot = nx * tx + ny * ty + nz * tz;
            tx -= nx * dot;
            ty -= ny * dot;
            tz -= nz * dot;
            float tLen = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
            if (tLen < 1e-6f) {
                float[] fallback = orthonormalTangent(nx, ny, nz);
                tx = fallback[0];
                ty = fallback[1];
                tz = fallback[2];
            } else {
                tx /= tLen;
                ty /= tLen;
                tz /= tLen;
            }
            float bx = ny * tz - nz * ty;
            float by = nz * tx - nx * tz;
            float bz = nx * ty - ny * tx;
            float bLen = (float) Math.sqrt(bx * bx + by * by + bz * bz);
            if (bLen < 1e-6f) {
                float[] fallback = orthonormalBitangent(nx, ny, nz, tx, ty, tz);
                bx = fallback[0];
                by = fallback[1];
                bz = fallback[2];
            } else {
                bx /= bLen;
                by /= bLen;
                bz /= bLen;
            }
            tangents[base] = tx;
            tangents[base + 1] = ty;
            tangents[base + 2] = tz;
            bitangents[base] = bx;
            bitangents[base + 1] = by;
            bitangents[base + 2] = bz;
        }
    }

    private static void add(float[] data, int index, float x, float y, float z) {
        int base = index * 3;
        data[base] += x;
        data[base + 1] += y;
        data[base + 2] += z;
    }

    private static void fillFallbackTangents(float[] normals, float[] tangents, float[] bitangents) {
        int vertexCount = normals.length / 3;
        for (int i = 0; i < vertexCount; i++) {
            int base = i * 3;
            float nx = normals[base];
            float ny = normals[base + 1];
            float nz = normals[base + 2];
            float[] t = orthonormalTangent(nx, ny, nz);
            tangents[base] = t[0];
            tangents[base + 1] = t[1];
            tangents[base + 2] = t[2];
            float[] b = orthonormalBitangent(nx, ny, nz, t[0], t[1], t[2]);
            bitangents[base] = b[0];
            bitangents[base + 1] = b[1];
            bitangents[base + 2] = b[2];
        }
    }

    /** Any unit vector perpendicular to the normal. */
    private static float[] orthonormalTangent(float nx, float ny, float nz) {
        float ax = Math.abs(nx);
        float az = Math.abs(nz);
        float ux = ax < az ? 1.0f : 0.0f;
        float uy = 0.0f;
        float uz = ax < az ? 0.0f : 1.0f;
        float tx = ny * uz - nz * uy;
        float ty = nz * ux - nx * uz;
        float tz = nx * uy - ny * ux;
        float len = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
        if (len < 1e-6f) {
            return new float[] { 1.0f, 0.0f, 0.0f };
        }
        return new float[] { tx / len, ty / len, tz / len };
    }

    private static float[] orthonormalBitangent(float nx, float ny, float nz, float tx, float ty, float tz) {
        float bx = ny * tz - nz * ty;
        float by = nz * tx - nx * tz;
        float bz = nx * ty - ny * tx;
        float len = (float) Math.sqrt(bx * bx + by * by + bz * bz);
        if (len < 1e-6f) {
            return new float[] { 0.0f, 1.0f, 0.0f };
        }
        return new float[] { bx / len, by / len, bz / len };
    }
}
