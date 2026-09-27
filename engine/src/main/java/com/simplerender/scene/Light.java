package com.simplerender.scene;

import java.util.List;

/**
 * A light source. Arrays are treated as read-only after construction.
 *
 * @param range         distance at which point/spot lights fade to zero (unused for directional)
 * @param innerConeCos  cosine of the spot cone angle with full intensity (spot only)
 * @param outerConeCos  cosine of the spot cone angle where light reaches zero (spot only)
 */
public record Light(
        Type type,
        float[] color,
        float[] position,
        float[] direction,
        float intensity,
        float range,
        float innerConeCos,
        float outerConeCos) {

    /** The ordinal is the light type constant used by the shaders (0, 1, 2). */
    public enum Type {
        DIRECTIONAL,
        POINT,
        SPOT
    }

    public static Light directional(float[] color, float[] direction, float intensity) {
        return new Light(Type.DIRECTIONAL, color, new float[] { 0.0f, 0.0f, 0.0f }, direction, intensity, 0.0f,
                0.0f, 0.0f);
    }

    public static Light point(float[] color, float[] position, float intensity, float range) {
        return new Light(Type.POINT, color, position, new float[] { 0.0f, -1.0f, 0.0f }, intensity, range,
                0.0f, 0.0f);
    }

    public static Light spot(
            float[] color,
            float[] position,
            float[] direction,
            float intensity,
            float range,
            float innerAngleDegrees,
            float outerAngleDegrees) {
        return new Light(Type.SPOT, color, position, direction, intensity, range,
                (float) Math.cos(Math.toRadians(innerAngleDegrees)),
                (float) Math.cos(Math.toRadians(outerAngleDegrees)));
    }

    /** A white key light, a warm fill light and a cool spot light. */
    public static List<Light> defaultRig() {
        return List.of(
                directional(new float[] { 1.0f, 1.0f, 1.0f }, new float[] { -0.3f, -1.0f, -0.2f }, 1.0f),
                point(new float[] { 1.0f, 0.75f, 0.6f }, new float[] { 1.5f, 1.2f, 1.0f }, 1.2f, 6.0f),
                spot(new float[] { 0.6f, 0.8f, 1.0f }, new float[] { -1.5f, 2.5f, 2.0f },
                        new float[] { 0.2f, -1.0f, -0.1f }, 1.4f, 8.0f, 12.5f, 18.0f));
    }
}
