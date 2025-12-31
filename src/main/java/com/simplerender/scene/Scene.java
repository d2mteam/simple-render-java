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

public final class Scene {
    private static final Logger logger = LoggerFactory.getLogger(Scene.class);

    private final Camera camera;
    private final CameraController cameraController;
    private final List<RenderItem> renderItems;

    private Scene(
        Camera camera,
        CameraController cameraController,
        List<RenderItem> renderItems
    ) {
        this.camera = camera;
        this.cameraController = cameraController;
        this.renderItems = renderItems;
    }

    public static Scene bootstrap(MeshUploader meshUploader, Optional<ModelImporter.ImportedModel> importedModel) {
        Camera camera = new Camera();
        CameraController cameraController = new CameraController();
        List<RenderItem> renderItems = new ArrayList<>();
        if (importedModel.isPresent()) {
            renderItems.add(createFromImported(meshUploader, importedModel.get()));
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
        RenderItem item = createFromImported(meshUploader, model);
        renderItems.add(item);
        int index = renderItems.size() - 1;
        logger.info("Added imported object at index {}", index);
        return index;
    }

    public int objectCount() {
        return renderItems.size();
    }

    private static RenderItem createFromImported(MeshUploader meshUploader, ModelImporter.ImportedModel model) {
        return new RenderItem(
            meshUploader.uploadMesh(model.meshData()),
            meshUploader.uploadMaterial(model.materialData()),
            new Transform()
        );
    }
}
