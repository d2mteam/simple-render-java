package com.simplerender.plugin.gltf;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;

/**
 * A loaded glTF file: the JSON description plus the raw bytes of its buffers, with helpers
 * to decode accessors (typed arrays inside the buffers).
 *
 * <p>Supports {@code .gltf} (JSON with external or {@code data:} URI buffers) and
 * {@code .glb} (binary container with an embedded buffer).
 */
final class GltfDocument {
    private static final int GLB_MAGIC = 0x46546C67; // "glTF"
    private static final int CHUNK_JSON = 0x4E4F534A; // "JSON"
    private static final int CHUNK_BIN = 0x004E4942;  // "BIN\0"

    // Accessor component types.
    private static final int BYTE = 5120;
    private static final int UNSIGNED_BYTE = 5121;
    private static final int SHORT = 5122;
    private static final int UNSIGNED_SHORT = 5123;
    private static final int UNSIGNED_INT = 5125;
    private static final int FLOAT = 5126;

    private final JsonObject root;
    private final byte[][] buffers;
    private final Path baseDir;

    private GltfDocument(JsonObject root, byte[][] buffers, Path baseDir) {
        this.root = root;
        this.buffers = buffers;
        this.baseDir = baseDir;
    }

    static GltfDocument load(Path path) throws IOException {
        Path baseDir = path.toAbsolutePath().getParent();
        if (path.toString().toLowerCase(Locale.ROOT).endsWith(".glb")) {
            return loadGlb(Files.readAllBytes(path), baseDir);
        }
        JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        return new GltfDocument(root, readBuffers(root, baseDir, null), baseDir);
    }

    private static GltfDocument loadGlb(byte[] bytes, Path baseDir) throws IOException {
        ByteBuffer data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        if (data.getInt() != GLB_MAGIC) {
            throw new IllegalArgumentException("Not a GLB file (bad header)");
        }
        data.getInt(); // version
        data.getInt(); // total length
        int jsonLength = data.getInt();
        if (data.getInt() != CHUNK_JSON) {
            throw new IllegalArgumentException("GLB file has no JSON chunk");
        }
        byte[] json = new byte[jsonLength];
        data.get(json);
        byte[] embedded = null;
        if (data.remaining() >= 8) {
            int binLength = data.getInt();
            if (data.getInt() == CHUNK_BIN) {
                embedded = new byte[binLength];
                data.get(embedded);
            }
        }
        JsonObject root = JsonParser.parseString(new String(json, StandardCharsets.UTF_8)).getAsJsonObject();
        return new GltfDocument(root, readBuffers(root, baseDir, embedded), baseDir);
    }

    /** Buffer 0 of a GLB has no URI: it is the embedded BIN chunk. */
    private static byte[][] readBuffers(JsonObject root, Path baseDir, byte[] embedded) throws IOException {
        JsonArray buffers = array(root, "buffers");
        byte[][] bytes = new byte[Math.max(buffers.size(), embedded != null ? 1 : 0)][];
        for (int i = 0; i < buffers.size(); i++) {
            JsonObject buffer = buffers.get(i).getAsJsonObject();
            if (buffer.has("uri")) {
                bytes[i] = readUri(buffer.get("uri").getAsString(), baseDir);
            }
        }
        if (embedded != null) {
            bytes[0] = embedded;
        }
        return bytes;
    }

    // ---- JSON access ------------------------------------------------------------------------

    JsonObject root() {
        return root;
    }

    /** {@code root[name][index]}, or {@code null} if it does not exist. */
    JsonObject object(String name, int index) {
        JsonArray items = array(root, name);
        return index >= 0 && index < items.size() ? items.get(index).getAsJsonObject() : null;
    }

    static JsonArray array(JsonObject owner, String name) {
        return owner.has(name) ? owner.getAsJsonArray(name) : new JsonArray();
    }

    static int intOr(JsonObject owner, String name, int fallback) {
        return owner.has(name) ? owner.get(name).getAsInt() : fallback;
    }

    // ---- Binary data ------------------------------------------------------------------------

    /** Bytes of a file referenced by a {@code uri} (relative path or {@code data:} URI). */
    byte[] readUri(String uri) throws IOException {
        return readUri(uri, baseDir);
    }

    private static byte[] readUri(String uri, Path baseDir) throws IOException {
        if (uri.startsWith("data:")) {
            return Base64.getDecoder().decode(uri.substring(uri.indexOf(',') + 1));
        }
        return Files.readAllBytes(baseDir.resolve(uri));
    }

    /** A copy of the bytes covered by a buffer view. */
    byte[] bufferViewBytes(int bufferViewIndex) {
        JsonObject view = object("bufferViews", bufferViewIndex);
        byte[] source = bufferOf(view);
        int offset = intOr(view, "byteOffset", 0);
        int length = view.get("byteLength").getAsInt();
        if (offset + length > source.length) {
            throw new IllegalArgumentException("bufferView " + bufferViewIndex + " exceeds its buffer");
        }
        byte[] slice = new byte[length];
        System.arraycopy(source, offset, slice, 0, length);
        return slice;
    }

    /**
     * Reads a float accessor ({@code components} values per element), converting integer and
     * normalised integer types to float and applying sparse overrides.
     */
    float[] readFloats(int accessorIndex, int components) {
        JsonObject accessor = object("accessors", accessorIndex);
        int count = accessor.get("count").getAsInt();
        int type = accessor.get("componentType").getAsInt();
        boolean normalized = accessor.has("normalized") && accessor.get("normalized").getAsBoolean();
        float[] values = new float[count * components]; // no bufferView: all zeros (sparse may fill in)

        if (accessor.has("bufferView")) {
            JsonObject view = object("bufferViews", accessor.get("bufferView").getAsInt());
            ByteBuffer data = wrap(bufferOf(view));
            int start = intOr(view, "byteOffset", 0) + intOr(accessor, "byteOffset", 0);
            int stride = intOr(view, "byteStride", componentSize(type) * components);
            for (int i = 0; i < count; i++) {
                for (int c = 0; c < components; c++) {
                    values[i * components + c] =
                            readFloat(data, start + i * stride + c * componentSize(type), type, normalized);
                }
            }
        }
        if (accessor.has("sparse")) {
            JsonObject sparse = accessor.getAsJsonObject("sparse");
            SparseReader reader = new SparseReader(sparse);
            for (int i = 0; i < reader.count; i++) {
                int target = reader.targetIndex(i);
                for (int c = 0; c < components; c++) {
                    int offset = reader.valuesOffset + (i * components + c) * componentSize(type);
                    values[target * components + c] = readFloat(reader.values, offset, type, normalized);
                }
            }
        }
        return values;
    }

    /** Reads an index accessor (unsigned byte, short or int), applying sparse overrides. */
    int[] readIndices(int accessorIndex) {
        JsonObject accessor = object("accessors", accessorIndex);
        int count = accessor.get("count").getAsInt();
        int type = accessor.get("componentType").getAsInt();
        int[] indices = new int[count];

        if (accessor.has("bufferView")) {
            JsonObject view = object("bufferViews", accessor.get("bufferView").getAsInt());
            ByteBuffer data = wrap(bufferOf(view));
            int start = intOr(view, "byteOffset", 0) + intOr(accessor, "byteOffset", 0);
            for (int i = 0; i < count; i++) {
                indices[i] = readIndex(data, start + i * componentSize(type), type);
            }
        }
        if (accessor.has("sparse")) {
            SparseReader reader = new SparseReader(accessor.getAsJsonObject("sparse"));
            for (int i = 0; i < reader.count; i++) {
                indices[reader.targetIndex(i)] =
                        readIndex(reader.values, reader.valuesOffset + i * componentSize(type), type);
            }
        }
        return indices;
    }

    /** Number of values per element for an accessor type such as {@code VEC3}. */
    static int componentCount(JsonObject accessor) {
        String type = accessor.get("type").getAsString();
        return switch (type) {
            case "SCALAR" -> 1;
            case "VEC2" -> 2;
            case "VEC3" -> 3;
            case "VEC4", "MAT2" -> 4;
            case "MAT3" -> 9;
            case "MAT4" -> 16;
            default -> throw new IllegalArgumentException("Unsupported accessor type: " + type);
        };
    }

    /** The two buffer views of a sparse accessor: which elements to replace, and with what. */
    private final class SparseReader {
        final int count;
        final ByteBuffer indices;
        final int indicesOffset;
        final int indexType;
        final ByteBuffer values;
        final int valuesOffset;

        SparseReader(JsonObject sparse) {
            JsonObject indicesInfo = sparse.getAsJsonObject("indices");
            JsonObject valuesInfo = sparse.getAsJsonObject("values");
            count = sparse.get("count").getAsInt();
            indices = wrap(bufferViewBytes(indicesInfo.get("bufferView").getAsInt()));
            indicesOffset = intOr(indicesInfo, "byteOffset", 0);
            indexType = indicesInfo.get("componentType").getAsInt();
            values = wrap(bufferViewBytes(valuesInfo.get("bufferView").getAsInt()));
            valuesOffset = intOr(valuesInfo, "byteOffset", 0);
        }

        int targetIndex(int i) {
            return readIndex(indices, indicesOffset + i * componentSize(indexType), indexType);
        }
    }

    private byte[] bufferOf(JsonObject bufferView) {
        int index = intOr(bufferView, "buffer", 0);
        if (index < 0 || index >= buffers.length || buffers[index] == null) {
            throw new IllegalArgumentException("glTF buffer " + index + " is missing");
        }
        return buffers[index];
    }

    private static ByteBuffer wrap(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    }

    private static int componentSize(int type) {
        return switch (type) {
            case BYTE, UNSIGNED_BYTE -> 1;
            case SHORT, UNSIGNED_SHORT -> 2;
            case UNSIGNED_INT, FLOAT -> 4;
            default -> throw new IllegalArgumentException("Unsupported component type " + type);
        };
    }

    /** Reads one component as float; normalised integers map to [0, 1] or [-1, 1]. */
    private static float readFloat(ByteBuffer data, int offset, int type, boolean normalized) {
        return switch (type) {
            case BYTE -> normalized ? Math.max(data.get(offset) / 127.0f, -1.0f) : data.get(offset);
            case UNSIGNED_BYTE -> normalized ? (data.get(offset) & 0xFF) / 255.0f : data.get(offset) & 0xFF;
            case SHORT -> normalized ? Math.max(data.getShort(offset) / 32767.0f, -1.0f) : data.getShort(offset);
            case UNSIGNED_SHORT -> normalized
                    ? (data.getShort(offset) & 0xFFFF) / 65535.0f
                    : data.getShort(offset) & 0xFFFF;
            case UNSIGNED_INT -> normalized
                    ? Integer.toUnsignedLong(data.getInt(offset)) / 4294967295.0f
                    : Integer.toUnsignedLong(data.getInt(offset));
            case FLOAT -> data.getFloat(offset);
            default -> throw new IllegalArgumentException("Unsupported component type " + type);
        };
    }

    private static int readIndex(ByteBuffer data, int offset, int type) {
        return switch (type) {
            case UNSIGNED_BYTE -> data.get(offset) & 0xFF;
            case UNSIGNED_SHORT -> data.getShort(offset) & 0xFFFF;
            case UNSIGNED_INT -> data.getInt(offset);
            default -> throw new IllegalArgumentException("Unsupported index component type " + type);
        };
    }
}
