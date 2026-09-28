package com.simplerender.scene;

import com.simplerender.math.Matrix4f;
import java.util.Arrays;

/**
 * Where an object sits in the world.
 *
 * <p>Combines two parts: the fixed transform that came with the imported model (for example
 * a glTF node hierarchy) and a user-editable position + uniform scale. The user part is the
 * <em>parent</em>, so scaling scales the whole model around the world origin.
 *
 * <p>Not thread-safe; {@link Scene} guards access.
 */
public final class Transform {
    private final float[] importedMatrix; // null means identity
    private float x;
    private float y;
    private float z;
    private float scale = 1.0f;

    public Transform() {
        this(null);
    }

    public Transform(float[] importedMatrix) {
        this.importedMatrix = importedMatrix == null ? null : Arrays.copyOf(importedMatrix, importedMatrix.length);
    }

    public void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    /** Column-major model matrix: {@code userTranslateScale * importedMatrix}. */
    public float[] matrix() {
        float[] user = new float[16];
        user[0] = scale;
        user[5] = scale;
        user[10] = scale;
        user[12] = x;
        user[13] = y;
        user[14] = z;
        user[15] = 1.0f;
        return importedMatrix == null ? user : Matrix4f.multiply(user, importedMatrix);
    }
}
