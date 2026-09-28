package com.simplerender.scene;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import com.simplerender.asset.MaterialData;
import com.simplerender.asset.MeshData;
import java.util.List;
import org.junit.jupiter.api.Test;

class SceneTest {
    private static final MeshData TRIANGLE = new MeshData(
            new float[] { 0, 0, 0, 1, 0, 0, 0, 1, 0 },
            new float[] { 0, 0, 1, 0, 0, 1, 0, 0, 1 },
            null,
            new int[] { 0, 1, 2 });
    private static final MaterialData GREY = new MaterialData(new float[] { 0.5f, 0.5f, 0.5f }, null);

    @Test
    void snapshotContainsObjectsWithTheirModelMatrices() {
        Scene scene = new Scene();
        int first = scene.addAll(List.of(
                new SceneObject("a", TRIANGLE, GREY, null),
                new SceneObject("b", TRIANGLE, GREY, null)));
        scene.setTransform(1, 1, 2, 3, 2);

        SceneSnapshot snapshot = scene.snapshot();
        assertEquals(0, first);
        assertEquals(List.of("a", "b"), scene.objectNames());
        assertEquals(2, snapshot.items().size());
        assertEquals(3, snapshot.lights().size());
        float[] model = snapshot.items().get(1).modelMatrix();
        assertArrayEquals(new float[] { 2, 0, 0, 0, 0, 2, 0, 0, 0, 0, 2, 0, 1, 2, 3, 1 }, model);
    }

    @Test
    void userTransformIsParentOfImportedTransform() {
        float[] imported = { 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 10, 0, 0, 1 }; // translate x+10
        Scene scene = new Scene();
        scene.addAll(List.of(new SceneObject("a", TRIANGLE, GREY, imported)));
        scene.setTransformOfAll(0, 5, 0, 0.5f);

        float[] model = scene.snapshot().items().get(0).modelMatrix();
        // Scaled around the world origin first, then moved: x = 10 * 0.5 + 0, y = 5.
        assertEquals(5.0f, model[12]);
        assertEquals(5.0f, model[13]);
        assertEquals(0.5f, model[0]);
    }

    @Test
    void snapshotsAreIndependentOfLaterEdits() {
        Scene scene = new Scene();
        scene.addAll(List.of(new SceneObject("a", TRIANGLE, GREY, null)));
        SceneSnapshot before = scene.snapshot();
        scene.setTransform(0, 7, 0, 0, 1);

        assertEquals(0.0f, before.items().get(0).modelMatrix()[12]);
        assertNotSame(before.items(), scene.snapshot().items());
    }
}
