package com.simplerender.scene;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The world being rendered: a camera, a list of objects and a set of lights.
 *
 * <p>Thread-safe: the UI thread adds objects and edits transforms while the engine thread
 * calls {@link #update} and {@link #snapshot} every frame. Every method is synchronized.
 */
public final class Scene {
    private static final Logger logger = LoggerFactory.getLogger(Scene.class);

    private final Camera camera;
    private final CameraController cameraController = new CameraController();
    private final List<SceneObject> objects = new ArrayList<>();
    private final List<Light> lights;

    public Scene() {
        this(new Camera());
    }

    /** Uses {@code camera} as the scene camera; don't change it directly after this. */
    public Scene(Camera camera) {
        this.camera = camera;
        this.lights = Light.defaultRig();
    }

    /** Advances the simulation by one frame (currently: moves the camera). */
    public synchronized void update(float deltaSeconds, InputState input) {
        cameraController.update(camera, deltaSeconds, input);
    }

    /** Copies the current state for the renderer. */
    public synchronized SceneSnapshot snapshot() {
        List<SceneSnapshot.RenderItem> items = new ArrayList<>(objects.size());
        for (SceneObject object : objects) {
            items.add(new SceneSnapshot.RenderItem(object.mesh(), object.material(), object.transform().matrix()));
        }
        return new SceneSnapshot(camera.snapshot(), lights, List.copyOf(items));
    }

    /** Adds objects and returns the index of the first one. */
    public synchronized int addAll(List<SceneObject> newObjects) {
        int firstIndex = objects.size();
        objects.addAll(newObjects);
        logger.info("Added {} object(s); scene now has {}", newObjects.size(), objects.size());
        return firstIndex;
    }

    public synchronized int objectCount() {
        return objects.size();
    }

    public synchronized List<String> objectNames() {
        return objects.stream().map(SceneObject::name).toList();
    }

    public synchronized void setTransform(int index, float x, float y, float z, float scale) {
        Transform transform = objects.get(index).transform();
        transform.setPosition(x, y, z);
        transform.setScale(scale);
    }

    /** Moves and scales every object together, as if the scene were one model. */
    public synchronized void setTransformOfAll(float x, float y, float z, float scale) {
        for (int i = 0; i < objects.size(); i++) {
            setTransform(i, x, y, z, scale);
        }
    }

    public synchronized float cameraMoveSpeed() {
        return camera.moveSpeed();
    }

    public synchronized void setCameraMoveSpeed(float unitsPerSecond) {
        camera.setMoveSpeed(unitsPerSecond);
    }
}
