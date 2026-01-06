package com.simplerender.asset;

import com.simplerender.math.Vector3f;
import java.util.Arrays;

public final class MeshData {
    private final float[] positions;
    private final float[] normals;
    private final float[] texCoords0;
    private final float[] texCoords1;
    private final float[] tangents;
    private final float[] bitangents;
    private final int[] indices;
    private final Vector3f boundsCenter;
    private final float boundsRadius;

    public MeshData(float[] positions, float[] normals, int[] indices) {
        this(positions, normals, null, null, indices);
    }

    public MeshData(float[] positions, float[] normals, float[] texCoords, int[] indices) {
        this(positions, normals, texCoords, null, indices);
    }

    public MeshData(
            float[] positions,
            float[] normals,
            float[] tangents,
            float[] bitangents,
            float[] texCoords0,
            float[] texCoords1,
            int[] indices) {
        this.positions = Arrays.copyOf(positions, positions.length);
        this.normals = Arrays.copyOf(normals, normals.length);
        this.tangents = Arrays.copyOf(tangents, tangents.length);
        this.bitangents = Arrays.copyOf(bitangents, bitangents.length);
        int vertexCount = this.positions.length / 3;
        this.texCoords0 = buildTexCoords(texCoords0, vertexCount, null);
        this.texCoords1 = buildTexCoords(texCoords1, vertexCount, this.texCoords0);
        this.indices = Arrays.copyOf(indices, indices.length);

        Vector3f center = computeBoundsCenter(this.positions);
        this.boundsCenter = center;
        this.boundsRadius = computeBoundsRadius(this.positions, center);
    }

    public MeshData(
            float[] positions,
            float[] normals,
            float[] texCoords0,
            float[] texCoords1,
            int[] indices) {
        this.positions = Arrays.copyOf(positions, positions.length);
        this.normals = Arrays.copyOf(normals, normals.length);
        int vertexCount = this.positions.length / 3;
        this.texCoords0 = buildTexCoords(texCoords0, vertexCount, null);
        this.texCoords1 = buildTexCoords(texCoords1, vertexCount, this.texCoords0);
        this.indices = Arrays.copyOf(indices, indices.length);
        float[][] tb = buildTangents(
                this.positions,
                this.normals,
                this.texCoords0,
                this.indices);
        this.tangents = tb[0];
        this.bitangents = tb[1];
        Vector3f center = computeBoundsCenter(this.positions);
        this.boundsCenter = center;
        this.boundsRadius = computeBoundsRadius(this.positions, center);
    }

    public float[] positions() {
        return Arrays.copyOf(positions, positions.length);
    }

    public float[] normals() {
        return Arrays.copyOf(normals, normals.length);
    }

    public float[] texCoords() {
        return Arrays.copyOf(texCoords0, texCoords0.length);
    }

    public float[] texCoords0() {
        return Arrays.copyOf(texCoords0, texCoords0.length);
    }

    public float[] texCoords1() {
        return Arrays.copyOf(texCoords1, texCoords1.length);
    }

    public float[] tangents() {
        return Arrays.copyOf(tangents, tangents.length);
    }

    public float[] bitangents() {
        return Arrays.copyOf(bitangents, bitangents.length);
    }

    public int[] indices() {
        return Arrays.copyOf(indices, indices.length);
    }

    public int vertexCount() {
        return positions.length / 3;
    }

    public Vector3f boundsCenter() {
        return boundsCenter.copy();
    }

    public float boundsRadius() {
        return boundsRadius;
    }

    private static Vector3f computeBoundsCenter(float[] positions) {
        if (positions.length < 3) {
            return new Vector3f(0.0f, 0.0f, 0.0f);
        }
        float minX = positions[0];
        float minY = positions[1];
        float minZ = positions[2];
        float maxX = positions[0];
        float maxY = positions[1];
        float maxZ = positions[2];
        for (int i = 3; i < positions.length; i += 3) {
            float x = positions[i];
            float y = positions[i + 1];
            float z = positions[i + 2];
            if (x < minX) {
                minX = x;
            }
            if (y < minY) {
                minY = y;
            }
            if (z < minZ) {
                minZ = z;
            }
            if (x > maxX) {
                maxX = x;
            }
            if (y > maxY) {
                maxY = y;
            }
            if (z > maxZ) {
                maxZ = z;
            }
        }
        return new Vector3f(
                (minX + maxX) * 0.5f,
                (minY + maxY) * 0.5f,
                (minZ + maxZ) * 0.5f);
    }

    private static float computeBoundsRadius(float[] positions, Vector3f center) {
        if (positions.length < 3) {
            return 0.0f;
        }
        float maxDistanceSq = 0.0f;
        for (int i = 0; i < positions.length; i += 3) {
            float dx = positions[i] - center.x();
            float dy = positions[i + 1] - center.y();
            float dz = positions[i + 2] - center.z();
            float distanceSq = dx * dx + dy * dy + dz * dz;
            if (distanceSq > maxDistanceSq) {
                maxDistanceSq = distanceSq;
            }
        }
        return (float) Math.sqrt(maxDistanceSq);
    }

    private static float[] buildTexCoords(float[] texCoords, int vertexCount, float[] fallbackSource) {
        int expectedLength = vertexCount * 2;
        if (texCoords == null || texCoords.length != expectedLength) {
            if (fallbackSource != null && fallbackSource.length == expectedLength) {
                return Arrays.copyOf(fallbackSource, expectedLength);
            }
            float[] fallback = new float[expectedLength];
            for (int i = 0; i < vertexCount; i++) {
                int base = i * 2;
                fallback[base] = 0.5f;
                fallback[base + 1] = 0.5f;
            }
            return fallback;
        }
        return Arrays.copyOf(texCoords, texCoords.length);
    }

    private static float[][] buildTangents(float[] positions, float[] normals, float[] texCoords, int[] indices) {
        int vertexCount = positions.length / 3;
        float[] tangents = new float[vertexCount * 3];
        float[] bitangents = new float[vertexCount * 3];
        if (indices.length < 3 || texCoords.length != vertexCount * 2) {
            fillFallbackTangents(normals, tangents, bitangents);
            return new float[][] { tangents, bitangents };
        }
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
            accumulate(tangents, i0, tx, ty, tz);
            accumulate(tangents, i1, tx, ty, tz);
            accumulate(tangents, i2, tx, ty, tz);
            accumulate(bitangents, i0, bx, by, bz);
            accumulate(bitangents, i1, bx, by, bz);
            accumulate(bitangents, i2, bx, by, bz);
        }
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
        return new float[][] { tangents, bitangents };
    }

    private static void accumulate(float[] data, int index, float x, float y, float z) {
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

    private static float[] orthonormalTangent(float nx, float ny, float nz) {
        float ax = Math.abs(nx);
        float ay = Math.abs(ny);
        float az = Math.abs(nz);
        float ux;
        float uy;
        float uz;
        if (ax < az) {
            ux = 1.0f;
            uy = 0.0f;
            uz = 0.0f;
        } else {
            ux = 0.0f;
            uy = 0.0f;
            uz = 1.0f;
        }
        float tx = ny * uz - nz * uy;
        float ty = nz * ux - nx * uz;
        float tz = nx * uy - ny * ux;
        float len = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
        if (len < 1e-6f) {
            return new float[] { 1.0f, 0.0f, 0.0f };
        }
        return new float[] { tx / len, ty / len, tz / len };
    }

    private static float[] orthonormalBitangent(
            float nx,
            float ny,
            float nz,
            float tx,
            float ty,
            float tz) {
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
