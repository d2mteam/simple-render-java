package com.simplerender.scene;

import com.simplerender.app.EngineConfig;
import com.simplerender.app.Time;
import com.simplerender.camera.Camera;
import com.simplerender.camera.CameraController;
import com.simplerender.world.ChunkMeshData;
import com.simplerender.world.ChunkMeshDataFactory;

public final class Scene {
    private final Camera camera;
    private final CameraController cameraController;
    private final RenderableChunk[] chunks;

    private Scene(Camera camera, CameraController cameraController, RenderableChunk[] chunks) {
        this.camera = camera;
        this.cameraController = cameraController;
        this.chunks = chunks;
    }

    public static Scene bootstrap(EngineConfig config) {
        Camera camera = new Camera();
        CameraController cameraController = new CameraController();
        ChunkMeshData[] meshData = ChunkMeshDataFactory.randomChunks(config.chunkCount(), config.randomSeed());
        RenderableChunk[] chunks = new RenderableChunk[meshData.length];
        for (int i = 0; i < meshData.length; i++) {
            chunks[i] = new RenderableChunk(meshData[i]);
        }
        return new Scene(camera, cameraController, chunks);
    }

    public void update(Time time) {
        cameraController.update(camera, time);
    }

    public SceneSnapshot snapshot() {
        ChunkMeshData[] snapshots = new ChunkMeshData[chunks.length];
        for (int i = 0; i < chunks.length; i++) {
            snapshots[i] = chunks[i].snapshot();
        }
        return new SceneSnapshot(camera.snapshot(), snapshots);
    }
}
