package com.simplerender.scene;

import com.simplerender.app.Time;
import com.simplerender.camera.Camera;
import com.simplerender.camera.CameraController;
import com.simplerender.world.ChunkMeshData;

public final class Scene {
    private final Camera camera;
    private final CameraController cameraController;
    private final RenderableChunk chunk;

    private Scene(Camera camera, CameraController cameraController, RenderableChunk chunk) {
        this.camera = camera;
        this.cameraController = cameraController;
        this.chunk = chunk;
    }

    public static Scene bootstrap() {
        Camera camera = new Camera();
        CameraController cameraController = new CameraController();
        RenderableChunk chunk = new RenderableChunk(ChunkMeshData.empty());
        return new Scene(camera, cameraController, chunk);
    }

    public void update(Time time) {
        cameraController.update(camera, time);
    }

    public SceneSnapshot snapshot() {
        return new SceneSnapshot(camera.snapshot(), chunk.snapshot());
    }
}
