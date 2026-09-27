package com.simplerender.scene;

import com.simplerender.asset.MaterialData;
import com.simplerender.asset.MeshData;

/**
 * One drawable thing in the scene: a mesh, the material it is drawn with, and its transform.
 *
 * <p>Holds plain CPU data only. The renderer uploads the mesh and material to the GPU the
 * first time the object is drawn, so creating objects never needs an OpenGL context.
 */
public final class SceneObject {
    private final String name;
    private final MeshData mesh;
    private final MaterialData material;
    private final Transform transform;

    /**
     * @param importedMatrix the object's transform from the model file, or {@code null}
     */
    public SceneObject(String name, MeshData mesh, MaterialData material, float[] importedMatrix) {
        this.name = name;
        this.mesh = mesh;
        this.material = material;
        this.transform = new Transform(importedMatrix);
    }

    public String name() {
        return name;
    }

    public MeshData mesh() {
        return mesh;
    }

    public MaterialData material() {
        return material;
    }

    Transform transform() {
        return transform;
    }
}
