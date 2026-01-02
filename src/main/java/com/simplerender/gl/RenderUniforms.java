package com.simplerender.gl;

import com.simplerender.math.Matrix4f;
import com.simplerender.math.Vector3f;

final class RenderUniforms {
    static final int MAX_LIGHTS = 8;
    static final int LIGHT_DIRECTIONAL = 0;
    static final int LIGHT_POINT = 1;
    static final int LIGHT_SPOT = 2;

    private final float[] viewMatrix = Matrix4f.identity();
    private final float[] projectionMatrix;
    private final Light[] lights = new Light[MAX_LIGHTS];
    private int lightCount;

    RenderUniforms(float aspect) {
        projectionMatrix = Matrix4f.perspective((float) Math.toRadians(60.0f), aspect, 0.1f, 100.0f);
        seedDefaultLights();
    }

    public float[] viewMatrix() {
        return viewMatrix;
    }

    public float[] projectionMatrix() {
        return projectionMatrix;
    }

    public int lightCount() {
        return lightCount;
    }

    public Light light(int index) {
        if (index < 0 || index >= lightCount) {
            throw new IllegalArgumentException("Light index out of range");
        }
        return lights[index];
    }

    public void updateView(Vector3f position, Vector3f forward, Vector3f up) {
        Vector3f center = position.add(forward);
        float[] updated = Matrix4f.lookAt(position, center, up);
        System.arraycopy(updated, 0, viewMatrix, 0, updated.length);
    }

    public void updateProjection(float aspect) {
        float[] updated = Matrix4f.perspective((float) Math.toRadians(60.0f), aspect, 0.1f, 100.0f);
        System.arraycopy(updated, 0, projectionMatrix, 0, updated.length);
    }

    private void seedDefaultLights() {
        lights[0] = new Light(
            LIGHT_DIRECTIONAL,
            new float[] { 1.0f, 1.0f, 1.0f },
            new float[] { 0.0f, 0.0f, 0.0f },
            new float[] { -0.3f, -1.0f, -0.2f },
            new float[] { 1.0f, 0.0f, 0.0f, 0.0f }
        );
        lights[1] = new Light(
            LIGHT_POINT,
            new float[] { 1.0f, 0.75f, 0.6f },
            new float[] { 1.5f, 1.2f, 1.0f },
            new float[] { 0.0f, -1.0f, 0.0f },
            new float[] { 1.2f, 6.0f, 0.0f, 0.0f }
        );
        float innerCos = (float) Math.cos(Math.toRadians(12.5f));
        float outerCos = (float) Math.cos(Math.toRadians(18.0f));
        lights[2] = new Light(
            LIGHT_SPOT,
            new float[] { 0.6f, 0.8f, 1.0f },
            new float[] { -1.5f, 2.5f, 2.0f },
            new float[] { 0.2f, -1.0f, -0.1f },
            new float[] { 1.4f, 8.0f, innerCos, outerCos }
        );
        lightCount = 3;
    }

    static final class Light {
        private final int type;
        private final float[] color;
        private final float[] position;
        private final float[] direction;
        private final float[] params;

        private Light(int type, float[] color, float[] position, float[] direction, float[] params) {
            this.type = type;
            this.color = color;
            this.position = position;
            this.direction = direction;
            this.params = params;
        }

        public int type() {
            return type;
        }

        public float[] color() {
            return color;
        }

        public float[] position() {
            return position;
        }

        public float[] direction() {
            return direction;
        }

        public float[] params() {
            return params;
        }
    }
}
