package com.simplerender.scene;

import com.simplerender.app.EngineConfig;
import com.simplerender.app.InputState;
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
import com.simplerender.render.Transform;
import com.simplerender.world.ChunkMeshData;
import com.simplerender.world.ChunkMeshDataFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Scene {
    private static final Logger logger = LoggerFactory.getLogger(Scene.class);

    private final Camera camera;
    private final CameraController cameraController;
    private final List<RenderableChunk> chunks;

    private Scene(Camera camera, CameraController cameraController, List<RenderableChunk> chunks) {
        this.camera = camera;
        this.cameraController = cameraController;
        this.chunks = chunks;
    }

    public static Scene bootstrap(EngineConfig config, MeshUploader meshUploader, Optional<ModelImporter.ImportedModel> importedModel) {
        Camera camera = new Camera();
        CameraController cameraController = new CameraController();
        List<RenderableChunk> chunks = importedModel
            .map(model -> List.of(createFromImported(meshUploader, model)))
            .orElseGet(() -> createRandomChunks(config, meshUploader));
        if (importedModel.isPresent()) {
            logger.info("Scene bootstrapped with imported model");
        } else {
            logger.info("Scene bootstrapped with default chunks");
        }
        logger.info("Scene bootstrapped with {} chunks", chunks.size());
        return new Scene(camera, cameraController, chunks);
    }

    public void update(Time time, InputState inputState) {
        cameraController.update(camera, time, inputState);
    }

    public SceneSnapshot snapshot() {
        RenderItem[] snapshots = new RenderItem[chunks.size()];
        for (int i = 0; i < chunks.size(); i++) {
            snapshots[i] = chunks.get(i).snapshot();
        }
        logger.debug("Scene snapshot created for {} chunks", snapshots.length);
        return new SceneSnapshot(camera.snapshot(), snapshots);
    }

    public void updatePrimaryTransform(float x, float y, float z, float scale) {
        updateTransform(0, x, y, z, scale);
    }

    public void updateTransform(int index, float x, float y, float z, float scale) {
        if (chunks.isEmpty()) {
            logger.warn("No renderable chunks available to update transform");
            return;
        }
        if (index < 0 || index >= chunks.size()) {
            logger.warn("Transform index {} out of range (count={})", index, chunks.size());
            return;
        }
        chunks.get(index).updateTransform(x, y, z, scale);
        logger.info("Updated object {} transform to ({}, {}, {}) scale {}", index, x, y, z, scale);
    }

    public int addImportedObject(MeshUploader meshUploader, ModelImporter.ImportedModel model) {
        RenderableChunk chunk = createFromImported(meshUploader, model);
        chunks.add(chunk);
        int index = chunks.size() - 1;
        logger.info("Added imported object at index {}", index);
        return index;
    }

    public int objectCount() {
        return chunks.size();
    }

    private static List<RenderableChunk> createRandomChunks(EngineConfig config, MeshUploader meshUploader) {
        TextureData textureData = TextureDataFactory.checkerboard(32, 4);
        ChunkMeshData[] meshData = ChunkMeshDataFactory.randomChunks(config.chunkCount(), config.randomSeed());
        List<RenderableChunk> chunks = new ArrayList<>(meshData.length);
        for (int i = 0; i < meshData.length; i++) {
            MeshData mesh = MeshDataFactory.fromChunkMeshData(meshData[i]);
            MaterialData material = new MaterialData(new float[] {0.2f, 0.8f, 0.4f}, textureData);
            MeshHandle meshHandle = meshUploader.uploadMesh(mesh);
            MaterialHandle materialHandle = meshUploader.uploadMaterial(material);
            chunks.add(new RenderableChunk(new RenderItem(meshHandle, materialHandle, new Transform())));
        }
        return chunks;
    }

    private static RenderableChunk createFromImported(MeshUploader meshUploader, ModelImporter.ImportedModel model) {
        MeshHandle meshHandle = meshUploader.uploadMesh(model.meshData());
        MaterialHandle materialHandle = meshUploader.uploadMaterial(model.materialData());
        return new RenderableChunk(new RenderItem(meshHandle, materialHandle, new Transform()));
    }
}
