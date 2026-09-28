package com.simplerender.plugin.gltf;

import com.openize.drako.Draco;
import com.openize.drako.DracoMesh;
import com.openize.drako.DracoPointCloud;
import com.openize.drako.DrakoException;
import com.openize.drako.PointAttribute;
import java.util.HashMap;
import java.util.Map;

/**
 * Decodes {@code KHR_draco_mesh_compression} data with Openize.Drako, a pure-Java port of
 * Google's Draco decoder (MIT licence, no native code, no other dependencies).
 */
final class DracoDecoder {
    /** Which Draco attribute (by unique id) holds a glTF attribute, and its expected size. */
    record AttributeSpec(int attributeId, int components, int count) {
    }

    /** Decoded vertex attributes by glTF name (e.g. {@code POSITION}) and triangle indices. */
    record DecodedDracoMesh(Map<String, float[]> attributes, int[] indices, int vertexCount) {
    }

    private DracoDecoder() {
    }

    static DecodedDracoMesh decode(byte[] compressed, Map<String, AttributeSpec> specs) {
        DracoPointCloud geometry;
        try {
            geometry = Draco.decode(compressed);
        } catch (DrakoException e) {
            throw new IllegalStateException("Invalid Draco data", e);
        }
        if (geometry == null) {
            return null;
        }
        int pointCount = geometry.getNumPoints();

        Map<String, float[]> attributes = new HashMap<>();
        for (Map.Entry<String, AttributeSpec> entry : specs.entrySet()) {
            PointAttribute attribute = findByUniqueId(geometry, entry.getValue().attributeId());
            if (attribute != null) {
                attributes.put(entry.getKey(), readAttribute(attribute, entry.getValue(), pointCount));
            }
        }
        int[] indices = geometry instanceof DracoMesh mesh ? readTriangles(mesh) : null;
        return new DecodedDracoMesh(attributes, indices, pointCount);
    }

    private static PointAttribute findByUniqueId(DracoPointCloud geometry, int uniqueId) {
        for (int i = 0; i < geometry.getNumAttributes(); i++) {
            PointAttribute attribute = geometry.attribute(i);
            if (attribute.getUniqueId() == uniqueId) {
                return attribute;
            }
        }
        return null;
    }

    /** One value per point, as floats (Draco undoes quantisation while decoding). */
    private static float[] readAttribute(PointAttribute attribute, AttributeSpec spec, int pointCount) {
        int components = spec.components();
        int count = spec.count() > 0 ? spec.count() : pointCount; // size the glTF accessor promises
        float[] values = new float[count * components];
        float[] value = new float[attribute.getComponentsCount()];
        for (int point = 0; point < Math.min(count, pointCount); point++) {
            attribute.getValue(attribute.mappedIndex(point), value);
            System.arraycopy(value, 0, values, point * components, Math.min(components, value.length));
        }
        return values;
    }

    private static int[] readTriangles(DracoMesh mesh) {
        int[] indices = new int[mesh.getNumFaces() * 3];
        int[] face = new int[3];
        for (int f = 0; f < mesh.getNumFaces(); f++) {
            mesh.readFace(f, face);
            System.arraycopy(face, 0, indices, f * 3, 3);
        }
        return indices;
    }
}
