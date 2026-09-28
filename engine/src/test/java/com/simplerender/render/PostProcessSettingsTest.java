package com.simplerender.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class PostProcessSettingsTest {
    @Test
    void copyCopiesEveryFieldAndIsIndependent() throws IllegalAccessException {
        PostProcessSettings original = new PostProcessSettings();
        // Give every field a non-default value, so a field forgotten in copy() is caught.
        for (Field field : PostProcessSettings.class.getFields()) {
            if (field.getType() == boolean.class) {
                field.setBoolean(original, !field.getBoolean(original));
            } else {
                field.setFloat(original, field.getFloat(original) + 0.25f);
            }
        }

        PostProcessSettings copy = original.copy();
        for (Field field : PostProcessSettings.class.getFields()) {
            assertEquals(field.get(original), field.get(copy), field.getName());
        }

        original.exposure = 42.0f;
        assertTrue(copy.exposure != 42.0f);
    }
}
