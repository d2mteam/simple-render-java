package com.simplerender.scene;

import com.simplerender.app.EngineConfig;
import com.simplerender.app.Time;
import com.simplerender.camera.Camera;
import com.simplerender.camera.CameraController;
import com.simplerender.world.ChunkMeshData;
import com.simplerender.world.ChunkMeshDataFactory;
import java.nio.file.Path;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Scene {
    private static final Logger logger = LoggerFactory.getLogger(Scene.class);

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
        ChunkMeshData[] meshData = loadMeshData(config);
        RenderableChunk[] chunks = new RenderableChunk[meshData.length];
        for (int i = 0; i < meshData.length; i++) {
            chunks[i] = new RenderableChunk(meshData[i]);
        }
        logger.info("Scene bootstrapped with {} chunks", chunks.length);
        return new Scene(camera, cameraController, chunks);
    }

    private static ChunkMeshData[] loadMeshData(EngineConfig config) {
        String modelPath = config.modelPath();
        if (modelPath == null || modelPath.isBlank()) {
            return ChunkMeshDataFactory.randomChunks(config.chunkCount(), config.randomSeed());
        }
        ChunkMeshData imported = importWithPlugin(Path.of(modelPath.trim()));
        if (imported == null) {
            logger.warn("Falling back to random chunks because model import failed");
            return ChunkMeshDataFactory.randomChunks(config.chunkCount(), config.randomSeed());
        }
        return new ChunkMeshData[] {imported};
    }

    private static ChunkMeshData importWithPlugin(Path modelPath) {
        try {
            Class<?> serviceClass = Class.forName("com.simplerender.app.ModelImportService");
            Object service = serviceClass.getDeclaredConstructor().newInstance();
            Method importMethod = serviceClass.getMethod("importModel", Path.class);
            Object result = importMethod.invoke(service, modelPath);
            if (result instanceof ChunkMeshData) {
                return (ChunkMeshData) result;
            }
            logger.error("ModelImportService returned unexpected type: {}", result);
        } catch (ClassNotFoundException ex) {
            logger.error("ModelImportService not found on classpath", ex);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException ex) {
            logger.error("Failed to initialize ModelImportService", ex);
        } catch (InvocationTargetException ex) {
            logger.error("ModelImportService importModel failed", ex.getTargetException());
        }
        return null;
    }

    public void update(Time time) {
        cameraController.update(camera, time);
    }

    public SceneSnapshot snapshot() {
        ChunkMeshData[] snapshots = new ChunkMeshData[chunks.length];
        for (int i = 0; i < chunks.length; i++) {
            snapshots[i] = chunks[i].snapshot();
        }
        logger.debug("Scene snapshot created for {} chunks", snapshots.length);
        return new SceneSnapshot(camera.snapshot(), snapshots);
    }
}
