package com.simplerender.gl;

import com.simplerender.math.Matrix4f;
import com.simplerender.math.Vector3f;

final class RenderUniforms {
    private final float[] viewMatrix = Matrix4f.identity();
    private final float[] projectionMatrix;
    private final float[] lightDirection = new float[] { -0.3f, -1.0f, -0.2f };

    RenderUniforms(float aspect) {
        projectionMatrix = Matrix4f.perspective((float) Math.toRadians(60.0f), aspect, 0.1f, 100.0f);
    }

    public float[] viewMatrix() {
        return viewMatrix;
    }

    public float[] projectionMatrix() {
        return projectionMatrix;
    }

    public float[] lightDirection() {
        return lightDirection;
    }

    public void updateView(Vector3f position, Vector3f forward, Vector3f up) {
        Vector3f center = position.add(forward);
        float[] updated = Matrix4f.lookAt(position, center, up);
        System.arraycopy(updated, 0, viewMatrix, 0, updated.length);
    }
}
