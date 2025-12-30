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
        if (config.shaderName() != null && !config.shaderName().isBlank()) {
            this.renderer.requestShader(config.shaderName());
        }
        this.renderer.init();
        ModelImportService importService = ModelImportService.defaultService();
        importService.loadPlugins();
        Optional<Path> modelPath = ModelFileDialog.chooseModelFile();
        modelPath.ifPresent(path -> logger.info("Selected model path: {}", path));
        Optional<ModelImporter.ImportedModel> importedModel = modelPath.flatMap(importService::importModel);
        if (modelPath.isPresent() && importedModel.isEmpty()) {
            throw new IllegalStateException("Selected model could not be imported.");
        }
        if (importedModel.isPresent()) {
            logger.info("Imported model ready for rendering");
        } else {
            logger.info("No model selected; using default chunks");
        }
        this.scene = Scene.bootstrap(config, renderer, importedModel);
        RenderControlPanel.launch(scene, renderer);
        this.gameLoop = new GameLoop(config, new Time(), scene, renderer);
    }

    public void run() {
        logger.info("GameApplication run started");
        gameLoop.run();
        logger.info("GameApplication run completed");
    }
}
