package com.simplerender.plugin.gltf;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

final class DracoDecoder {
    private static final String RESOURCE_BASE = "/META-INF/resources/webjars/draco3d/1.5.7/";
    private static final String DECODER_JS = RESOURCE_BASE + "draco_decoder_nodejs.js";
    private static final String DECODER_WASM = RESOURCE_BASE + "draco_decoder.wasm";
    private static final Object LOCK = new Object();
    private static DracoDecoder instance;

    private final Context context;
    private final Value decoderModule;

    static DracoDecoder getInstance() {
        synchronized (LOCK) {
            if (instance == null) {
                instance = new DracoDecoder();
            }
            return instance;
        }
    }

    private DracoDecoder() {
        try {
            this.context = Context.newBuilder("js")
                    .allowAllAccess(true)
                    .build();
            this.context.eval("js", "var module = { exports: {} }; var exports = module.exports; "
                    + "var process = undefined; var window = undefined; var importScripts = undefined;");
            String decoderSource = readResource(DECODER_JS);
            this.context.eval(Source.newBuilder("js", decoderSource, "draco_decoder_nodejs.js").build());
            byte[] wasmBytes = readResourceBytes(DECODER_WASM);
            this.context.getBindings("js").putMember("wasmBytes", wasmBytes);
            this.context.eval("js", "var wasmBinary = new Uint8Array(wasmBytes);");
            Value moduleFactory = context.getBindings("js").getMember("module").getMember("exports");
            Value config = context.eval("js", "({ wasmBinary: wasmBinary })");
            Value promise = moduleFactory.execute(config);
            this.decoderModule = awaitPromise(promise);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize Draco decoder", e);
        }
    }

    DecodedDracoMesh decode(byte[] compressed, Map<String, GltfModelImporter.DracoAttributeSpec> attributeSpecs) {
        Value decoder = decoderModule.getMember("Decoder").newInstance();
        Value buffer = decoderModule.getMember("DecoderBuffer").newInstance();
        context.getBindings("js").putMember("compressedBytes", compressed);
        Value byteArray = context.eval("js", "new Int8Array(compressedBytes)");
        buffer.invokeMember("Init", byteArray, compressed.length);
        int geometryType = decoder.invokeMember("GetEncodedGeometryType", buffer).asInt();
        int meshType = decoderModule.getMember("TRIANGULAR_MESH").asInt();
        int pointType = decoderModule.getMember("POINT_CLOUD").asInt();
        Value geometry;
        if (geometryType == meshType) {
            geometry = decoderModule.getMember("Mesh").newInstance();
            Value status = decoder.invokeMember("DecodeBufferToMesh", buffer, geometry);
            ensureStatusOk(status);
        } else if (geometryType == pointType) {
            geometry = decoderModule.getMember("PointCloud").newInstance();
            Value status = decoder.invokeMember("DecodeBufferToPointCloud", buffer, geometry);
            ensureStatusOk(status);
        } else {
            decoderModule.invokeMember("destroy", buffer);
            decoderModule.invokeMember("destroy", decoder);
            return null;
        }
        int vertexCount = geometry.invokeMember("num_points").asInt();
        Map<String, float[]> attributes = new HashMap<>();
        for (Map.Entry<String, GltfModelImporter.DracoAttributeSpec> entry : attributeSpecs.entrySet()) {
            GltfModelImporter.DracoAttributeSpec spec = entry.getValue();
            Value attribute = decoder.invokeMember("GetAttributeByUniqueId", geometry, spec.attributeId());
            if (attribute.isNull()) {
                continue;
            }
            Value data = decoderModule.getMember("DracoFloat32Array").newInstance();
            decoder.invokeMember("GetAttributeFloatForAllPoints", geometry, attribute, data);
            int size = data.invokeMember("size").asInt();
            float[] values = new float[size];
            for (int i = 0; i < size; i++) {
                values[i] = data.invokeMember("GetValue", i).asFloat();
            }
            decoderModule.invokeMember("destroy", data);
            int expectedSize = spec.count() * spec.components();
            if (expectedSize > 0 && expectedSize != values.length) {
                float[] resized = new float[expectedSize];
                System.arraycopy(values, 0, resized, 0, Math.min(values.length, resized.length));
                values = resized;
            }
            attributes.put(entry.getKey(), values);
        }
        int[] indices = geometryType == meshType ? readIndices(decoder, geometry) : null;
        decoderModule.invokeMember("destroy", buffer);
        decoderModule.invokeMember("destroy", geometry);
        decoderModule.invokeMember("destroy", decoder);
        return new DecodedDracoMesh(attributes, indices, vertexCount);
    }

    private int[] readIndices(Value decoder, Value mesh) {
        int numFaces = mesh.invokeMember("num_faces").asInt();
        int[] indices = new int[numFaces * 3];
        Value ia = decoderModule.getMember("DracoInt32Array").newInstance();
        for (int i = 0; i < numFaces; i++) {
            decoder.invokeMember("GetFaceFromMesh", mesh, i, ia);
            int base = i * 3;
            indices[base] = ia.invokeMember("GetValue", 0).asInt();
            indices[base + 1] = ia.invokeMember("GetValue", 1).asInt();
            indices[base + 2] = ia.invokeMember("GetValue", 2).asInt();
        }
        decoderModule.invokeMember("destroy", ia);
        return indices;
    }

    private void ensureStatusOk(Value status) {
        if (status.hasMember("ok") && !status.invokeMember("ok").asBoolean()) {
            throw new IllegalStateException("Draco decode failed: " + status.invokeMember("error_msg").asString());
        }
    }

    private Value awaitPromise(Value promise) throws Exception {
        if (!promise.hasMember("then")) {
            return promise;
        }
        CompletableFuture<Value> future = new CompletableFuture<>();
        PromiseCallback onFulfilled = new PromiseCallback(future, null);
        PromiseCallback onRejected = new PromiseCallback(future, new RuntimeException("Draco module initialization"));
        promise.invokeMember("then", onFulfilled, onRejected);
        return future.get(30, TimeUnit.SECONDS);
    }

    private String readResource(String path) throws IOException {
        try (InputStream stream = DracoDecoder.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Missing resource: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private byte[] readResourceBytes(String path) throws IOException {
        try (InputStream stream = DracoDecoder.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Missing resource: " + path);
            }
            return stream.readAllBytes();
        }
    }

    private static final class PromiseCallback implements ProxyExecutable {
        private final CompletableFuture<Value> future;
        private final RuntimeException baseException;

        private PromiseCallback(CompletableFuture<Value> future, RuntimeException baseException) {
            this.future = future;
            this.baseException = baseException;
        }

        @Override
        public Object execute(Value... arguments) {
            if (baseException == null) {
                future.complete(arguments[0]);
            } else {
                RuntimeException ex = baseException;
                if (arguments.length > 0) {
                    ex = new RuntimeException(arguments[0].toString(), ex);
                }
                future.completeExceptionally(ex);
            }
            return null;
        }
    }

    record DecodedDracoMesh(Map<String, float[]> attributes, int[] indices, int vertexCount) {
    }
}
