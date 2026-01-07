package com.simplerender.scene;

import com.simplerender.app.InputState;
import com.simplerender.app.Time;
import com.simplerender.asset.plugin.ModelImporter;
import com.simplerender.camera.Camera;
import com.simplerender.camera.CameraController;
import com.simplerender.render.MeshUploader;
import com.simplerender.render.RenderItem;
import com.simplerender.render.Transform;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runtime scene container that owns the active camera and renderable items.
 *
 * <p>The scene is responsible for bootstrapping imported models, updating camera
 * motion in response to input, and producing immutable {@link SceneSnapshot}
 * instances for the renderer thread.
 */
public final class Scene {
    private static final Logger logger = LoggerFactory.getLogger(Scene.class);

    private final Camera camera;
    private final CameraController cameraController;
    private final List<RenderItem> renderItems;

    private Scene(
            Camera camera,
            CameraController cameraController,
            List<RenderItem> renderItems) {
        this.camera = camera;
        this.cameraController = cameraController;
        this.renderItems = renderItems;
    }

    public Camera camera() {
        return camera;
    }

    public static Scene bootstrap(MeshUploader meshUploader, Optional<ModelImporter.ImportedModel> importedModel) {
        Camera camera = new Camera();
        CameraController cameraController = new CameraController();
        List<RenderItem> renderItems = new ArrayList<>();
        if (importedModel.isPresent()) {
            renderItems.addAll(createFromImported(meshUploader, importedModel.get()));
            logger.info("Scene bootstrapped with imported model");
        } else {
            logger.info("Scene bootstrapped with no renderable objects");
        }
        logger.info("Scene bootstrapped with {} render items", renderItems.size());
        return new Scene(camera, cameraController, renderItems);
    }

    public void update(Time time) {
        update(time, InputState.idle());
    }

    public void update(Time time, InputState inputState) {
        cameraController.update(camera, time, inputState);
    }

    public SceneSnapshot snapshot() {
        return new SceneSnapshot(camera.snapshot(), renderItems.toArray(new RenderItem[0]));
    }

    public void updatePrimaryTransform(float x, float y, float z, float scale) {
        updateTransform(0, x, y, z, scale);
    }

    public void updateTransform(int index, float x, float y, float z, float scale) {
        if (renderItems.isEmpty()) {
            logger.warn("No renderable objects available to update transform");
            return;
        }
        if (index < 0 || index >= objectCount()) {
            logger.warn("Transform index {} out of range (count={})", index, objectCount());
            return;
        }
        RenderItem item = renderItems.get(index);
        item.transform().setPosition(x, y, z);
        item.transform().setScale(scale);
        logger.info("Updated object {} transform to ({}, {}, {}) scale {}", index, x, y, z, scale);
    }

    public int addImportedObject(MeshUploader meshUploader, ModelImporter.ImportedModel model) {
        int startIndex = renderItems.size();
        List<RenderItem> items = createFromImported(meshUploader, model);
        if (items.isEmpty()) {
            logger.warn("Imported model contained no renderable primitives");
            return -1;
        }
        renderItems.addAll(items);
        logger.info("Added imported object(s) at index {}", startIndex);
        return startIndex;
    }

    public int objectCount() {
        return renderItems.size();
    }

    private static List<RenderItem> createFromImported(MeshUploader meshUploader, ModelImporter.ImportedModel model) {
        List<RenderItem> items = new ArrayList<>();
        for (ModelImporter.ImportedPrimitive primitive : model.primitives()) {
            Transform t = new Transform(primitive.transform());
            // t.setScale(0.1f); // REMOVED: This causes exploded view (scales mesh but not
            // position)
            items.add(new RenderItem(
                    meshUploader.uploadMesh(primitive.meshData()),
                    meshUploader.uploadMaterial(primitive.materialData()),
                    t));
        }
        return items;
    }
}
