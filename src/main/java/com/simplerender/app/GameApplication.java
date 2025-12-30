package com.simplerender.app;

import com.simplerender.gl.OpenGLRenderer;
import com.simplerender.scene.Scene;
import com.simplerender.asset.plugin.ModelImporter;

import java.nio.file.Path;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GameApplication {
    private static final Logger logger = LoggerFactory.getLogger(GameApplication.class);

    private final EngineConfig config;
    private final GameLoop gameLoop;
    private final Scene scene;
    private final OpenGLRenderer renderer;

    public GameApplication(EngineConfig config) {
        this.config = config;
        this.renderer = new OpenGLRenderer(config.chunkCount());
        ModelImportService importService = ModelImportService.defaultService();
        importService.loadPlugins();
        Optional<Path> modelPath = Optional.ofNullable(config.modelPath())
            .map(Path::of)
            .or(() -> ModelFileDialog.chooseModelFile());
        Optional<ModelImporter.ImportedModel> importedModel = modelPath.flatMap(importService::importModel);
        if (modelPath.isPresent() && importedModel.isEmpty()) {
            throw new IllegalStateException("Selected model could not be imported.");
        }
        this.scene = Scene.bootstrap(config, renderer, importedModel);
        this.gameLoop = new GameLoop(config, new Time(), scene, renderer);
    }

    public void run() {
        logger.info("GameApplication run started");
        gameLoop.run();
        logger.info("GameApplication run completed");
    }
}
