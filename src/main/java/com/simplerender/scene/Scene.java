package com.simplerender.scene;

import com.simplerender.app.EngineConfig;
import com.simplerender.app.ModelImportService;
import com.simplerender.app.Time;
import com.simplerender.asset.MaterialData;
import com.simplerender.asset.MeshData;
import com.simplerender.asset.MeshDataFactory;
import com.simplerender.asset.TextureData;
import com.simplerender.asset.TextureDataFactory;
import com.simplerender.asset.plugin.ModelImporter;
import com.simplerender.camera.Camera;
import com.simplerender.camera.CameraController;
import com.simplerender.render.MaterialHandle;
import com.simplerender.render.MeshHandle;
import com.simplerender.render.MeshUploader;
import com.simplerender.render.RenderItem;
import com.simplerender.world.ChunkMeshData;
import com.simplerender.world.ChunkMeshDataFactory;
import java.nio.file.Path;
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

    public static Scene bootstrap(EngineConfig config, MeshUploader meshUploader, Optional<ModelImporter.ImportedModel> importedModel) {
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
        ModelImportService importService = new ModelImportService();
        ChunkMeshData imported = importService.importModel(Path.of(modelPath.trim()));
        if (imported == null) {
            logger.warn("Falling back to random chunks because model import failed");
            return ChunkMeshDataFactory.randomChunks(config.chunkCount(), config.randomSeed());
        }
        return new ChunkMeshData[] {imported};
    }

    public void update(Time time) {
        cameraController.update(camera, time);
    }

    public SceneSnapshot snapshot() {
        RenderItem[] snapshots = new RenderItem[chunks.length];
        for (int i = 0; i < chunks.length; i++) {
            snapshots[i] = chunks[i].snapshot();
        }
        logger.debug("Scene snapshot created for {} chunks", snapshots.length);
        return new SceneSnapshot(camera.snapshot(), snapshots);
    }

    private static RenderableChunk[] createRandomChunks(EngineConfig config, MeshUploader meshUploader) {
        TextureData textureData = TextureDataFactory.checkerboard(32, 4);
        ChunkMeshData[] meshData = ChunkMeshDataFactory.randomChunks(config.chunkCount(), config.randomSeed());
        RenderableChunk[] chunks = new RenderableChunk[meshData.length];
        for (int i = 0; i < meshData.length; i++) {
            MeshData mesh = MeshDataFactory.fromChunkMeshData(meshData[i]);
            MaterialData material = new MaterialData(new float[] {0.2f, 0.8f, 0.4f}, textureData);
            MeshHandle meshHandle = meshUploader.uploadMesh(mesh);
            MaterialHandle materialHandle = meshUploader.uploadMaterial(material);
            chunks[i] = new RenderableChunk(new RenderItem(meshHandle, materialHandle));
        }
        return chunks;
    }

    private static RenderableChunk createFromImported(MeshUploader meshUploader, ModelImporter.ImportedModel model) {
        MeshHandle meshHandle = meshUploader.uploadMesh(model.meshData());
        MaterialHandle materialHandle = meshUploader.uploadMaterial(model.materialData());
        return new RenderableChunk(new RenderItem(meshHandle, materialHandle));
    }
}
